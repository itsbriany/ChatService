package com.chat

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.Write
import akka.util.ByteString
import com.chat.message.{ActorClient, AddActorClient, FindActorClient, RemoveActorClient}

/**
  * Created by itsbriany on 2016-08-30.
  */
class ClientIdentityResolver extends Actor {
  type Identity = String

  var clientIdentityMap = new collection.mutable.HashMap[Identity, ActorRef]()

  override def receive: Receive = {
    case addActorClient: AddActorClient =>
      handleAddClientIdentity(addActorClient.getClientIdentity)
    case removeActorClient: RemoveActorClient =>
      handleRemoveClientIdentity(removeActorClient.getClientIdentity)
    case findActorClient: FindActorClient =>
      handleFindClientIdentity(findActorClient.getClientIdentity)
  }

  def handleAddClientIdentity(client: ActorClient): Unit = {
    if (clientIdentityMap contains client.getIdentity) {
      client.getConnection ! Write(ClientIdentityResolver.identityAlreadyExistsMessage(client))
      return
    }

    clientIdentityMap += client.getIdentity -> client.getConnection
    client.getConnection ! Write(ClientIdentityResolver.greeting(client.getIdentity))
  }

  def handleRemoveClientIdentity(client: ActorClient): Unit = {
    clientIdentityMap -= client.getIdentity
  }

  def handleFindClientIdentity(client: ActorClient): Unit = {
    clientIdentityMap.get(client.getIdentity) match {
      case Some(clientActor) => sender ! clientActor
      case None => sender ! None
    }
  }
}

object ClientIdentityResolver {
  def identityAlreadyExistsMessage(client: ActorClient): ByteString =
    ByteString(s"${client.getIdentity} already exists!")

  def greeting(identity: String): ByteString = ByteString(s"Welcome $identity!")
}