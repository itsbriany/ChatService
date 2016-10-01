package com.chat

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import GameEngine.Common.chat.{ChatMessage, Identity}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.Write
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.chat.message.{ActorClient, FindActorClient}

import scala.concurrent.Await

/**
  * Created by itsbriany on 2016-09-22.
  */
class ResponseBroadcaster(connection: ActorRef, clientIdentityResolver: ActorRef)
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case chatMessage: ChatMessage => handleChatMessage(chatMessage)
  }

  def handleChatMessage(chatMessage: ChatMessage): Unit = {
    chatMessage.destination match {
      case Some(destinationIdentity) =>
        val destinationConnection = findDestination(destinationIdentity)
        destinationConnection match {
          case Some(destination) =>
            broadcastFormattedResponse(connection, destination, chatMessage)
          case None =>
            connection ! Write(ClientConnection.unresolvedDestinationReply(destinationIdentity.name))
        }
      case None => connection ! Write(ClientConnection.missingDestinationReply)
    }
  }

  def broadcastFormattedResponse(source: ActorRef, destination: ActorRef, chatMessage: ChatMessage): Unit = {
    val formattedResponse = formatResponse(chatMessage, Calendar.getInstance.getTime)
    source ! Write(formattedResponse)
    destination ! Write(formattedResponse)
  }

  def formatResponse(chatMessage: ChatMessage, timestamp: Date): ByteString = {
    val formatter = new SimpleDateFormat("HH:mm:ss")
    val timestampAsString = formatter.format(timestamp)

    var sourceName: String = "Unknown"
    chatMessage.source match {
      case Some(identity) =>
        if (!identity.name.isEmpty) sourceName = identity.name
      case None =>
    }

    // [DateFormat Source] Message
    val buffer = new StringBuilder
    buffer += '['
    buffer ++= timestampAsString
    buffer += ' '
    buffer ++= sourceName
    buffer ++= "] "
    buffer ++= chatMessage.text

    ByteString(buffer.toString())
  }

  def findDestination(destination: Identity): Option[ActorRef] = {
    val destinationActorClient = new ActorClient(destination, null)
    val findActorClient = new FindActorClient(destinationActorClient)

    implicit val timeout = Timeout(ClientConnection.resolveDestinationActorTimeout)
    val findActorClientFuture = clientIdentityResolver ? findActorClient
    val result =
      Await.result(findActorClientFuture, ClientConnection.resolveDestinationActorTimeout)

    result match {
      case destinationActor: ActorRef => Some(destinationActor)
      case _ => None
    }
  }
}
