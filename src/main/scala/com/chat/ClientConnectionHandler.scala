package com.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import com.chat.message.{ActorClient, AddActorClient, FindActorClient, RemoveActorClient}

import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class ClientConnectionHandler(connection: ActorRef,
                              address: InetSocketAddress,
                              clientIdentityResolver: ActorRef)
  extends Actor with ActorLogging {

  var client = new ActorClient("", self)
  var destinationConnection = connection

  def receive = {
    case destinationConnection: ActorRef => this.destinationConnection = destinationConnection
    case findClientIdentity: FindActorClient => clientIdentityResolver ! findClientIdentity
    case addClientIdentity: AddActorClient => handleClientIdentity(addClientIdentity)
    case data: ByteString => handleData(data)
    case Received(data) => handleData(data)
    case PeerClosed => handlePeerClosed()
  }

  def handleClientIdentity(addClientIdentity: AddActorClient): Unit = {
    this.client = addClientIdentity.getClientIdentity
    clientIdentityResolver ! addClientIdentity
  }

  def handleData(data: ByteString): Unit = {
    if (this.client.isIdentityEmpty) {
      connection ! Write(ClientConnectionHandler.missingIdentityReply)
      return
    }
    writeData(data)
  }

  def writeData(data: ByteString): Unit = {
    connection ! Write(data)
    if (destinationConnection != self)
      destinationConnection ! Write(data)
  }

  def handlePeerClosed(): Unit = {
    val removeClientIdentity = new RemoveActorClient(client)
    clientIdentityResolver ! removeClientIdentity
    log.info(s"$address has disconnected")
    context stop self
  }
}

object ClientConnectionHandler {
  val addClientIdentityFutureTimeout = 500.millis

  def missingIdentityReply: ByteString =
    ByteString("Please specify a Client Identity before sending messages\n")
}

