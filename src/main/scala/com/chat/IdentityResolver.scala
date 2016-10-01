package com.chat

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.Write
import akka.util.ByteString
import com.chat.message.{ActorClient, AddActorClient, FindActorClient, RemoveActorClient}

/**
  * Created by itsbriany on 2016-08-30.
  */
class IdentityResolver extends Actor {

  var clientIdentityMap = new collection.mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {
    case addActorClient: AddActorClient =>
      handleAddClientIdentity(addActorClient.getClientIdentity)
    case removeActorClient: RemoveActorClient =>
      handleRemoveClientIdentity(removeActorClient.getClientIdentity)
    case findActorClient: FindActorClient =>
      handleFindClientIdentity(findActorClient.getClientIdentity)
  }

  def handleAddClientIdentity(client: ActorClient): Unit = {
    if (clientIdentityMap contains client.getIdentity.name) {
      client.getActorRef ! Write(IdentityResolver.identityAlreadyExistsMessage(client))
      return
    }

    clientIdentityMap += client.getIdentity.name -> client.getActorRef
    client.getActorRef ! Write(IdentityResolver.greeting(client.getIdentity.name))
  }

  def handleRemoveClientIdentity(client: ActorClient): Unit = {
    clientIdentityMap -= client.getIdentity.name
  }

  def handleFindClientIdentity(client: ActorClient): Unit = {
    clientIdentityMap.get(client.getIdentity.name) match {
      case Some(clientActor) => sender ! clientActor
      case None => sender ! None
    }
  }
}

object IdentityResolver {
  def identityAlreadyExistsMessage(client: ActorClient): ByteString =
    ByteString(s"${client.getIdentity} already exists!")

  def greeting(identity: String): ByteString = ByteString(s"Welcome $identity!")
}