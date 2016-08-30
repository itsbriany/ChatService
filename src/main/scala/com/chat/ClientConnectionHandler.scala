package com.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import com.chat.message.ClientIdentity

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class ClientConnectionHandler(connection: ActorRef, address: InetSocketAddress) extends Actor
  with ActorLogging {

  var clientIdentity = new ClientIdentity("", address)

  def receive = {
    case text: String => handleString(text)
    case clientIdentity: ClientIdentity => handleClientIdentity(clientIdentity)
    case Received(data) => connection ! data
    case PeerClosed => context stop self
  }

  def handleClientIdentity(clientIdentity: ClientIdentity): Unit = {
    this.clientIdentity = clientIdentity
    connection ! Write(ClientConnectionHandler.greeting(this.clientIdentity.getIdentity))
  }

  def handleString(data: String): Unit = {
    if (this.clientIdentity.isEmpty)
      connection ! Write(ClientConnectionHandler.missingIdentityReply)
    else
      connection ! Write(ByteString(data))
  }
}

object ClientConnectionHandler {
  def greeting(identity: String): ByteString = ByteString(s"Welcome $identity!")
  def missingIdentityReply: ByteString =
    ByteString("Please specify a Client Identity before sending messages")
}

