package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.chat.{ChatMessage, Connect, Identity}
import akka.actor.ActorSystem
import akka.io.Tcp.Write
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import com.chat.message.ActorClient
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by itsbriany on 2016-08-29.
  */
class ClientConnectionHandlerSpec extends TestKit(ActorSystem())
  with WordSpecLike
  with Matchers
  with BeforeAndAfterEach {

  val address = new InetSocketAddress("0.0.0.0", 0)
  var identityResolver = TestProbe()
  var sourceClient = TestProbe()
  var clientConnection =
    TestActorRef(new ClientConnection(sourceClient.ref, address, identityResolver.ref))
  var destinationClient = TestProbe()
  var destinationConnectionHandler =
    TestActorRef(new ClientConnection(destinationClient.ref, address, identityResolver.ref))
  var responseBroadcaster = TestProbe()

  override def beforeEach(): Unit = {
    val destinationIdentity = new Identity("Destination")
    destinationClient = TestProbe()
    identityResolver = TestProbe()
    destinationConnectionHandler =
      TestActorRef(new ClientConnection(destinationClient.ref, address, identityResolver.ref))
    val destinationActorClient = new ActorClient(destinationIdentity, destinationClient.ref)
    destinationConnectionHandler.underlyingActor.actorClient = Some(destinationActorClient)

    sourceClient = TestProbe()
    clientConnection = TestActorRef(new ClientConnection(sourceClient.ref, address, identityResolver.ref))
    responseBroadcaster = TestProbe()
    clientConnection.underlyingActor.responseBroadcaster = responseBroadcaster.ref
  }

  s"A ${ClientConnection.getClass.getSimpleName}" must {
    s"set its identity when receiving a ${Connect.getClass.getSimpleName} message" in {
      val clientIdentity = new Identity("Brian")
      val connectMessage = new Connect(Some(clientIdentity))
      val chatMessageAsByteString = ByteString(connectMessage.toByteArray)
      clientConnection.tell(chatMessageAsByteString, sourceClient.ref)
      clientConnection.underlyingActor.actorClient.get.getIdentity.name shouldBe clientIdentity.name
    }

    "let the client know that it needs to set the source in the ChatMessage if it has not already been done" in {
      val source = None
      val destination = None
      val chatMessage = new ChatMessage(source, destination, "Hello!")
      val chatMessageAsByteString = ByteString(chatMessage.toByteArray)
      clientConnection.tell(chatMessageAsByteString, sourceClient.ref)
      sourceClient.expectMsg(200.millis, Write(ClientConnection.missingIdentityReply))
    }

    s"let the client know that message format sent is not a ${ChatMessage.getClass.getSimpleName}" in {
      val byteStringMessage = ByteString("Hello!")
      clientConnection.tell(byteStringMessage, sourceClient.ref)
      sourceClient.expectMsg(200.millis, Write(ClientConnection.invalidMessageReply))
    }

    "be capable of proxying a message to its ResponseBroadcaster" in {
      val sourceIdentity = Identity("Source")
      val destination = Identity("Destination")
      val payload = "I am the text!"

      clientConnection.underlyingActor.actorClient =
        Some(new ActorClient(sourceIdentity, sourceClient.ref))

      val chatMessage =
        ChatMessage(Option[Identity](sourceIdentity), Option[Identity](destination), payload)
      val chatMessageAsByteString = ByteString(chatMessage.toByteArray)

      clientConnection.tell(chatMessageAsByteString, sourceClient.ref)
      responseBroadcaster.expectMsg(200.millis, chatMessage)
    }
  }
}
