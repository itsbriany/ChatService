package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.chat.{ChatMessage, Connect, Identity}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import com.chat.message._
import com.google.protobuf.InvalidProtocolBufferException

import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class ClientConnection(connection: ActorRef,
                       address: InetSocketAddress,
                       clientIdentityResolver: ActorRef)
  extends Actor with ActorLogging {

  var responseBroadcaster =
    context.actorOf(Props(new ResponseBroadcaster(connection, clientIdentityResolver)))
  var actorClient: Option[ActorClient] = None
  var destinationConnection = connection

  def receive = {
    case identity: Identity => handleClientIdentity(identity)
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
      actorClient match {
        case Some(value) =>
          val chatMessage: ChatMessage = ChatMessage.parseFrom(data.toArray)
          broadcastChatMessage(chatMessage)
        case None =>
          val connect: Connect = Connect.parseFrom(data.toArray)
          connectClient(connect)
      }
    } catch {
      case ex: InvalidProtocolBufferException =>
        connection ! Write(ClientConnection.invalidMessageReply)
    }
  }

  def connectClient(connect: Connect) = {
    connect.identity match {
      case Some(identity) => handleClientIdentity(identity)
      case None => connection ! Write(ClientConnection.missingIdentityReply)
    }
  }

  def handleClientIdentity(identity: Identity): Unit = {
    if (identity.name.isEmpty) {
      connection ! ClientConnection.missingIdentityNameReply
      return
    }
    actorClient = Some(new ActorClient(identity, connection))
    clientIdentityResolver ! new AddActorClient(actorClient.get)
  }

  def broadcastChatMessage(chatMessage: ChatMessage): Unit = {
    responseBroadcaster ! chatMessage
  }

  def handlePeerClosed(): Unit = {
    actorClient match {
      case Some(value) =>
        val removeClientIdentity = new RemoveActorClient(value)
        clientIdentityResolver ! removeClientIdentity
      case None =>
    }

    log.info(s"$address has disconnected")
    context stop self
  }
}

object ClientConnection {
  val resolveDestinationActorTimeout = 250.millis
  val setActorClientTimeout = 250.millis

  def invalidMessageReply: ByteString =
    ByteString(s"Invalid message")

  def missingIdentityReply: ByteString =
    ByteString("Missing an identity\n")

  def missingIdentityNameReply: ByteString =
    ByteString("Identity is missing a name")

  def unresolvedDestinationReply(destinationIdentity: String) =
    ByteString(s"It looks like $destinationIdentity is offline")

  def missingDestinationReply: ByteString = ByteString(s"Field missing: Destination")
}

