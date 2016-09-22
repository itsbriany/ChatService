package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.chat.{ChatMessage, ClientIdentity}
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
    case data: ByteString => handleData(data)
    case Received(data) => handleData(data)
    case PeerClosed => handlePeerClosed()
  }

  /**
    * Handle all data that comes over the wire
    *
    * @param data The data coming over the wire as a ByteString
    */
  def handleData(data: ByteString): Unit = {
    // TODO
    // Attempt to serialize the data as a Chat Message
    // Let the client know that the message was not a chat message on failure
    // Reply back to the client in the following format: [Date ClientIdentity] Message
    // Extra: it may be nice to be format the message
    try {
      val chatMessage: ChatMessage = ChatMessage.parseFrom(data.toArray)
      if (actorClient.isIdentityEmpty) {
        handleClientIdentity(chatMessage.getSource)
      }
      findDestination(chatMessage.getDestination)
      val dataToWrite = ByteString(chatMessage.text)
      writeData(dataToWrite)
    } catch {
      case ex: InvalidProtocolBufferException =>
        connection ! Write(ClientConnectionHandler.notAChatMessageReply)
    }
  }

  def findDestination(destination: ClientIdentity): Unit = {
    if (destination.identity.isEmpty) {
      connection ! Write(ClientConnectionHandler.missingDestinationReply)
      return
    }

    val destinationActorClient = new ActorClient(destination.identity, null)
    val findActorClient = new FindActorClient(destinationActorClient)

    implicit val timeout = Timeout(ClientConnectionHandler.resolveDestinationActorTimeout)
    val findActorClientFuture = clientIdentityResolver ? findActorClient
    val result =
      Await.result(findActorClientFuture, ClientConnectionHandler.resolveDestinationActorTimeout)

    result match {
      case clientActor: ActorRef => this.destinationConnection = clientActor
      case _ =>
        connection ! Write(ClientConnectionHandler.unresolvedDestinationReply(destination.identity))
    }
  }

  def writeData(data: ByteString): Unit = {
    connection ! Write(data)
    if (destinationConnection != self)
      destinationConnection ! Write(data)
  }

  def handleClientIdentity(clientIdentity: ClientIdentity): Unit = {
    if (clientIdentity.identity.isEmpty) {
      connection ! Write(ClientConnectionHandler.missingSourceReply)
      return
    }

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
    ByteString(s"Not a ${ChatMessage.getClass.getSimpleName}")

  def missingSourceReply: ByteString =
    ByteString("Field missing: Source\n")

  def unresolvedDestinationReply(destinationIdentity: String) =
    ByteString(s"It looks like $destinationIdentity is offline")

  def missingDestinationReply: ByteString = ByteString(s"Field missing: Destination")
}

