package com.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem}
import akka.io.Tcp.Write
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import com.chat.generated.common.{ClientIdentity, SetDestination}
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

  val mockClientIdentityResolver = TestActorRef(new MockClientIdentityResolver())
  val address = new InetSocketAddress("0.0.0.0", 0)
  var client = TestProbe()
  var clientConnectionHandler =
    TestActorRef(new ClientConnectionHandler(client.ref, address, mockClientIdentityResolver))

  override def beforeEach(): Unit = {
    client = TestProbe()
    clientConnectionHandler =
      TestActorRef(new ClientConnectionHandler(client.ref, address, mockClientIdentityResolver))
  }

  class MockClientIdentityResolver extends Actor {
    val mockConnection = TestProbe().ref
    val expectedIdentity = "JonJon"

    override def receive: Receive = {
      case findActorClient: FindActorClient =>
        if (findActorClient.getClientIdentity.getIdentity == expectedIdentity)
          sender ! mockConnection
        else
          sender ! None
    }
  }

  s"A ${ClientConnectionHandler.getClass.getSimpleName}" must {
    s"set its identity when receiving a ${ClientIdentity.getClass.getSimpleName} message" in {
      val expectedIdentity = "Brian"
      val clientIdentity = new ClientIdentity(expectedIdentity)
      clientConnectionHandler.tell(clientIdentity, client.ref)
      clientConnectionHandler.underlyingActor.actorClient.getIdentity shouldBe expectedIdentity
    }

    "receive data from the client and reply to it when it has received an identity" in {
      val actorClient = new ActorClient("Brian", client.ref)
      clientConnectionHandler.underlyingActor.actorClient = actorClient

      val byteStringMessage = ByteString("Hello!")
      clientConnectionHandler.tell(byteStringMessage, client.ref)
      client.expectMsg(200.millis, Write(byteStringMessage))
    }

    "let the client know that they need to specify an identity when none is provided" in {
      val byteStringMessage = ByteString("Hello!")
      clientConnectionHandler.tell(byteStringMessage, client.ref)
      client.expectMsg(200.millis, Write(ClientConnectionHandler.missingIdentityReply))
    }

    "let the client choose its destination to send its messages to" in {
      val destinationClientIdentity = new ClientIdentity("JonJon")
      val setDestinationMessage = new SetDestination(Option[ClientIdentity](destinationClientIdentity))
      clientConnectionHandler.tell(setDestinationMessage, client.ref)

      clientConnectionHandler.underlyingActor.destinationConnection shouldBe mockClientIdentityResolver.underlyingActor.mockConnection
    }

    "let the client know when the client's desired destination is unreachable" in {
      val identity = "Not JonJon"
      val destinationClientIdentity = new ClientIdentity(identity)
      val setDestinationMessage = new SetDestination(Option[ClientIdentity](destinationClientIdentity))
      clientConnectionHandler.tell(setDestinationMessage, client.ref)

      client.expectMsg(200.millis, Write(ClientConnectionHandler.unresolvedDestinationReply(identity)))
      clientConnectionHandler.underlyingActor.destinationConnection shouldBe client.ref
    }

    "be capable of sending a string message to itself and another client" in {
      val destinationConnection = TestProbe()
      val message = "Message sent to destination"
      val expectedResponse = Write(ByteString(message))

      clientConnectionHandler.underlyingActor.destinationConnection = destinationConnection.ref
      clientConnectionHandler.underlyingActor.writeData(ByteString(message))

      client.expectMsg(200.millis, expectedResponse)
      destinationConnection.expectMsg(200.millis, expectedResponse)
      client.expectNoMsg(200.millis)
      destinationConnection.expectNoMsg(200.millis)
    }
  }
}
