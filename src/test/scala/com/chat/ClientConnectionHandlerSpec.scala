package com.chat

import java.net.InetSocketAddress

import GameEngine.Common.chat.{ChatMessage, ClientIdentity}
import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.io.Tcp.Write
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import com.chat.message.{ActorClient, FindActorClient}
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
  var mockClientIdentityResolver = TestActorRef(new MockClientIdentityResolver(null, null))
  var sourceClient = TestProbe()
  var clientConnectionHandler =
    TestActorRef(new ClientConnectionHandler(sourceClient.ref, address, mockClientIdentityResolver))
  var destinationClient = TestProbe()
  var destinationConnectionHandler =
    TestActorRef(new ClientConnectionHandler(destinationClient.ref, address, mockClientIdentityResolver))
  var responseBroadcaster = TestProbe()

  override def beforeEach(): Unit = {
    destinationClient = TestProbe()
    val destinationId = "Destination"
    mockClientIdentityResolver = TestActorRef(new MockClientIdentityResolver(destinationClient.ref, destinationId))

    destinationConnectionHandler =
      TestActorRef(new ClientConnectionHandler(destinationClient.ref, address, mockClientIdentityResolver))
    val destinationActorClient = new ActorClient(destinationId, destinationClient.ref)
    destinationConnectionHandler.underlyingActor.actorClient = destinationActorClient

    sourceClient = TestProbe()
    clientConnectionHandler =
      TestActorRef(new ClientConnectionHandler(sourceClient.ref, address, mockClientIdentityResolver))
    responseBroadcaster = TestProbe()
    clientConnectionHandler.underlyingActor.responseBroadcaster = responseBroadcaster.ref
  }


  class MockClientIdentityResolver(destinationTestProbeRef: ActorRef, id: String) extends Actor {
    val mockConnection = destinationTestProbeRef
    val expectedIdentity = id

    override def receive: Receive = {
      case findActorClient: FindActorClient =>
        if (findActorClient.getClientIdentity.getIdentity == expectedIdentity)
          sender ! mockConnection
        else
          sender ! None
    }
  }

  s"A ${ClientConnectionHandler.getClass.getSimpleName}" must {
    s"set its identity when receiving a ${ChatMessage.getClass.getSimpleName} message" in {
      val expectedIdentity = "Brian"
      val clientIdentity = new ClientIdentity(expectedIdentity)
      val chatMessage =
        new ChatMessage(Option[ClientIdentity](clientIdentity), Option[ClientIdentity](null), "")
      val chatMessageAsByteString = ByteString(chatMessage.toByteArray)
      clientConnectionHandler.tell(chatMessageAsByteString, sourceClient.ref)
      clientConnectionHandler.underlyingActor.actorClient.getIdentity shouldBe expectedIdentity
    }

    "let the client know that it needs to set the source in the ChatMessage if it has not already been done" in {
      val source = None
      val destination = None
      val chatMessage = new ChatMessage(source, destination, "Hello!")
      val chatMessageAsByteString = ByteString(chatMessage.toByteArray)
      clientConnectionHandler.tell(chatMessageAsByteString, sourceClient.ref)
      sourceClient.expectMsg(200.millis, Write(ClientConnectionHandler.missingSourceReply))
    }

    s"let the client know that message format sent is not a ${ChatMessage.getClass.getSimpleName}" in {
      val byteStringMessage = ByteString("Hello!")
      clientConnectionHandler.tell(byteStringMessage, sourceClient.ref)
      sourceClient.expectMsg(200.millis, Write(ClientConnectionHandler.notAChatMessageReply))
    }

    "be capable of proxying a message to its ResponseBroadcaster" in {
      val sourceIdentity = ClientIdentity("Source")
      val destination = ClientIdentity("Destination")
      val payload = "I am the text!"
      val chatMessage =
        ChatMessage(Option[ClientIdentity](sourceIdentity), Option[ClientIdentity](destination), payload)
      val chatMessageAsByteString = ByteString(chatMessage.toByteArray)

      clientConnectionHandler.tell(chatMessageAsByteString, sourceClient.ref)
      responseBroadcaster.expectMsg(200.millis, chatMessage)
    }
  }
}
