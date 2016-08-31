package com.chat

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.Write
import akka.util.ByteString
import com.chat.message.{AddClientIdentity, ClientIdentity, FindClientIdentity, RemoveClientIdentity}

/**
  * Created by itsbriany on 2016-08-30.
  */
class ClientIdentityResolver extends Actor {
  type Identity = String

  var clientIdentityMap = new collection.mutable.HashMap[Identity, ActorRef]()

  override def receive: Receive = {
    case addClientIdentity: AddClientIdentity =>
      handleAddClientIdentity(addClientIdentity.getClientIdentity)
    case removeClientIdentity: RemoveClientIdentity =>
      handleRemoveClientIdentity(removeClientIdentity.getClientIdentity)
    case findClientIdentity: FindClientIdentity =>
      handleFindClientIdentity(findClientIdentity.getClientIdentity)
  }

  def handleAddClientIdentity(clientIdentity: ClientIdentity): Unit = {
    if (clientIdentityMap contains clientIdentity.getIdentity) {
      clientIdentity.getConnection ! Write(ClientIdentityResolver.identityAlreadyExistsMessage(clientIdentity))
      return
    }

    clientIdentityMap += clientIdentity.getIdentity -> clientIdentity.getConnection
    clientIdentity.getConnection ! Write(ClientIdentityResolver.greeting(clientIdentity.getIdentity))
  }

  def handleRemoveClientIdentity(clientIdentity: ClientIdentity): Unit = {
    clientIdentityMap -= clientIdentity.getIdentity
  }

  def handleFindClientIdentity(clientIdentity: ClientIdentity): Unit = {
    clientIdentityMap.get(clientIdentity.getIdentity) match {
      case Some(inetSocketAddress) => sender ! inetSocketAddress
      case None => sender ! None
    }
  }
}

object ClientIdentityResolver {
  def identityAlreadyExistsMessage(clientIdentity: ClientIdentity): ByteString =
    ByteString(s"${clientIdentity.getIdentity} already exists!")

  def greeting(identity: String): ByteString = ByteString(s"Welcome $identity!")
}