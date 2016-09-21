package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.Chat.{ClientIdentity, SetDestination}
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

  def handleData(data: ByteString): Unit = {
    if (actorClient.isIdentityEmpty) {
      if (!isClientIdentityMessage(data)) {
        connection ! Write(ClientConnectionHandler.missingIdentityReply)
        return
      }
    }
    writeData(data)
  }

  def isClientIdentityMessage(data: ByteString): Boolean = {
    val byteArray = data.toArray
    try {
      val clientIdentity: ClientIdentity = ClientIdentity.parseFrom(byteArray)
      handleClientIdentity(clientIdentity)
      true
    } catch {
      case ex: InvalidProtocolBufferException => false
    }
  }

  def handleClientIdentity(clientIdentity: ClientIdentity): Unit = {
    actorClient = new ActorClient(clientIdentity.identity, self)
    val addActorClient = new AddActorClient(actorClient)
    clientIdentityResolver ! addActorClient
  }

  def writeData(data: ByteString): Unit = {
    connection ! Write(data)
    if (destinationConnection != self)
      destinationConnection ! Write(data)
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

  def missingIdentityReply: ByteString =
    ByteString("Please specify a Client Identity before sending messages\n")

  def unresolvedDestinationReply(destinationIdentity: String) =
    ByteString(s"It looks like $destinationIdentity is offline")
}

