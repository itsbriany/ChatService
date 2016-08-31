package com.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem}
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

    override def receive: Receive = {
      case findClientIdentity: FindActorClient => sender ! mockConnection
    }
  }

  s"A ${ClientConnectionHandler.getClass.getSimpleName}" must {
    "receive data from the client and reply to it when it has received an identity" in {
      val actorClient = new ActorClient("Brian", client.ref)
      clientConnectionHandler.underlyingActor.client = actorClient

      val byteStringMessage = ByteString("Hello!")
      clientConnectionHandler.tell(byteStringMessage, client.ref)
      client.expectMsg(200.millis, Write(byteStringMessage))
    }

    "let the client know that they need to specify an identity when none is provided" in {
      val byteStringMessage = ByteString("Hello!")
      clientConnectionHandler.tell(byteStringMessage, client.ref)
      client.expectMsg(200.millis, Write(ClientConnectionHandler.missingIdentityReply))
    }

    "be capable of requesting a destination to send a message to" in {
      val findActorClient = new FindActorClient(null)
      clientConnectionHandler.tell(findActorClient, client.ref)

      clientConnectionHandler.underlyingActor.destinationConnection shouldBe mockClientIdentityResolver.underlyingActor.mockConnection
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
