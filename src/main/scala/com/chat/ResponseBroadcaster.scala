package com.chat

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import GameEngine.Common.chat.{ChatMessage, ClientIdentity}
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
          case Some(destination) => broadcastFormattedResponse(connection, destination, chatMessage)
          case None => connection ! Write(ClientConnectionHandler.unresolvedDestinationReply(destinationIdentity.identity))
        }
      case None => connection ! Write(ClientConnectionHandler.missingDestinationReply)
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

    // [DateFormat Source] Message
    val buffer = new StringBuilder
    buffer += '['
    buffer ++= timestampAsString
    buffer += ' '
    buffer ++= chatMessage.getSource.identity
    buffer ++= "] "
    buffer ++= chatMessage.text

    ByteString(buffer.toString())
  }

  def findDestination(destination: ClientIdentity): Option[ActorRef] = {
    val destinationActorClient = new ActorClient(destination.identity, null)
    val findActorClient = new FindActorClient(destinationActorClient)

    implicit val timeout = Timeout(ClientConnectionHandler.resolveDestinationActorTimeout)
    val findActorClientFuture = clientIdentityResolver ? findActorClient
    val result =
      Await.result(findActorClientFuture, ClientConnectionHandler.resolveDestinationActorTimeout)

    result match {
      case destinationActor: ActorRef => Some(destinationActor)
      case _ => None
    }
  }
}
