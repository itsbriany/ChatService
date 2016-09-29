package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.chat.{ChatMessage, ClientIdentity}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import com.chat.message._
import com.google.protobuf.InvalidProtocolBufferException

import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class ClientConnectionHandler(connection: ActorRef,
                              address: InetSocketAddress,
                              clientIdentityResolver: ActorRef)
  extends Actor with ActorLogging {

  var responseBroadcaster =
    context.actorOf(Props(new ResponseBroadcaster(connection, clientIdentityResolver)))
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
    try {
      val chatMessage: ChatMessage = ChatMessage.parseFrom(data.toArray)
      if (actorClient.isIdentityEmpty) {
        handleClientIdentity(chatMessage.getSource)
      }
      broadcastChatMessage(chatMessage)
    } catch {
      case ex: InvalidProtocolBufferException =>
        connection ! Write(ClientConnectionHandler.notAChatMessageReply)
    }
  }

  def broadcastChatMessage(chatMessage: ChatMessage): Unit = {
    responseBroadcaster ! chatMessage
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

