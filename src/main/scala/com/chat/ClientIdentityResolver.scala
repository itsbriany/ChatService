package com.chat

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.util.ByteString
import com.chat.message.{AddClientIdentity, ClientIdentity, FindClientIdentity, RemoveClientIdentity}

/**
  * Created by itsbriany on 2016-08-30.
  */
class ClientIdentityResolver extends Actor {
  type Identity = String

  var clientIdentityMap = new collection.mutable.HashMap[Identity, InetSocketAddress]()

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
      sender() ! ClientIdentityResolver.identityAlreadyExistsMessage(clientIdentity)
      return
    }

    clientIdentityMap += clientIdentity.getIdentity -> clientIdentity.getAddress
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
}