package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.chat.{ChatMessage, ClientIdentity, SetDestination}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.chat.message.{ActorClient, AddActorClient, FindActorClient, RemoveActorClient}
import com.google.protobuf.InvalidProtocolBufferException

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class ClientConnectionHandler(connection: ActorRef,
                              address: InetSocketAddress,
                              clientIdentityResolver: ActorRef)
  extends Actor with ActorLogging {

  var actorClient = new ActorClient("", self)
  var destinationConnection = connection

  def receive = {
    case clientIdentity: ClientIdentity => handleClientIdentity(clientIdentity)
    case setDestination: SetDestination => handleSetDestination(setDestination)
    case data: ByteString => handleData(data)
    case Received(data) => handleData(data)
    case PeerClosed => handlePeerClosed()
  }

  def handleSetDestination(setDestination: SetDestination): Unit = {
    val destinationIdentity = setDestination.getClientIdentity.identity
    val destinationActorClient = new ActorClient(destinationIdentity, null)
    val findActorClient = new FindActorClient(destinationActorClient)

    implicit val timeout = Timeout(ClientConnectionHandler.resolveDestinationActorTimeout)
    val findActorClientFuture = clientIdentityResolver ? findActorClient
    val result =
      Await.result(findActorClientFuture, ClientConnectionHandler.resolveDestinationActorTimeout)

    result match {
      case clientActor: ActorRef => this.destinationConnection = clientActor
      case _ =>
        connection ! Write(ClientConnectionHandler.unresolvedDestinationReply(destinationIdentity))
    }
  }

  /**
    * Handle all data that comes over the wire
    *
    * @param data The data coming over the wire as a ByteString
    */
  def handleData(data: ByteString): Unit = {
    // Attempt to serialize the data as a Chat Message
    // Let the client know that the message was not a chat message on failure
    // Reply back to the client in the following format: [Date ClientIdentity] Message
    // Extra: it may be nice to be format the message
    if (actorClient.isIdentityEmpty) {
      if (!isChatMessage(data)) {
        connection ! Write(ClientConnectionHandler.notAChatMessageReply)
        return
      }
    }
    writeData(data)
  }

  def isChatMessage(data: ByteString): Boolean = {
    val byteArray = data.toArray
    try {
      val chatMessage: ChatMessage = ChatMessage.parseFrom(byteArray)
      handleClientIdentity(chatMessage.getSource)
      true
    } catch {
      case ex: InvalidProtocolBufferException => false
    }
  }

  def writeData(data: ByteString): Unit = {
    connection ! Write(data)
    if (destinationConnection != self)
      destinationConnection ! Write(data)
  }

  def handleClientIdentity(clientIdentity: ClientIdentity): Unit = {
    actorClient = new ActorClient(clientIdentity.identity, self)
    val addActorClient = new AddActorClient(actorClient)
    clientIdentityResolver ! addActorClient
  }

  def handlePeerClosed(): Unit = {
    val removeClientIdentity = new RemoveActorClient(actorClient)
    clientIdentityResolver ! removeClientIdentity
    log.info(s"$address has disconnected")
    context stop self
  }
}

object ClientConnectionHandler {
  val resolveDestinationActorTimeout = 250.millis

  def notAChatMessageReply: ByteString =
    ByteString("Not a chat message\n")

  def unresolvedDestinationReply(destinationIdentity: String) =
    ByteString(s"It looks like $destinationIdentity is offline")
}

