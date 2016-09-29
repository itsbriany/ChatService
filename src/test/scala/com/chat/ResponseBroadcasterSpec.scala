package com.chat

import java.text.SimpleDateFormat
import java.util.Calendar

import GameEngine.Common.chat.{ChatMessage, ClientIdentity}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.Tcp.Write
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.{ByteString, Timeout}
import com.chat.message.{ActorClient, AddActorClient}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by itsbriany on 2016-09-22.
  */
class ResponseBroadcasterSpec extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterEach {


  var clientConnection = TestProbe()
  var destinationConnection = TestProbe()
  var clientIdentityResolver = TestActorRef(Props[ClientIdentityResolver])
  var responseBroadcaster =
    TestActorRef(new ResponseBroadcaster(clientConnection.ref, clientIdentityResolver))

  var sourceClientIdentity = new ClientIdentity("Source")
  var destinationClientIdentity = new ClientIdentity("Destination")
  var payload = "Some text message"
  var chatMessage =
    new ChatMessage(Option[ClientIdentity](sourceClientIdentity), Option[ClientIdentity](destinationClientIdentity), payload)

  override def beforeEach(): Unit = {
    clientConnection = TestProbe()
    destinationConnection = TestProbe()
    clientIdentityResolver = TestActorRef(Props[ClientIdentityResolver])
    responseBroadcaster =
      TestActorRef(new ResponseBroadcaster(clientConnection.ref, clientIdentityResolver))

    sourceClientIdentity = new ClientIdentity("Source")
    destinationClientIdentity = new ClientIdentity("Destination")
    payload = "Some text message"
    chatMessage = new ChatMessage(Option[ClientIdentity](sourceClientIdentity),
      Option[ClientIdentity](destinationClientIdentity), payload)

    setupClientIdentityResolver()
  }

  def setupClientIdentityResolver(): Unit = {
    implicit val timeout = Timeout(ClientConnectionHandler.resolveDestinationActorTimeout)
    val destinationActorClient =
      new ActorClient(destinationClientIdentity.identity, destinationConnection.ref)
    val addActorClient = new AddActorClient(destinationActorClient)
    clientIdentityResolver ? addActorClient
  }

  s"A ResponseBroadcaster" must {

    "format responses from chat messages before broadcasting" in {
      val formatter = new SimpleDateFormat("HH:mm:ss")
      val timestamp = Calendar.getInstance().getTime
      val response = "[" + formatter.format(timestamp) + " Source] Some text message"
      val expected = ByteString(response)
      responseBroadcaster.underlyingActor.formatResponse(chatMessage, timestamp) shouldBe expected
    }

    "find a destination connection given an identity" in {
      val destination: Option[ActorRef] =
        responseBroadcaster.underlyingActor.findDestination(destinationClientIdentity)

      destination match {
        case Some(actorRef) => actorRef shouldBe destinationConnection.ref
        case None => fail("Expected an actor ref")
      }
    }

    "let the sender know that they must specify a destination" in {
      val destination = None
      chatMessage = new ChatMessage(Option[ClientIdentity](sourceClientIdentity), destination, payload)
      responseBroadcaster ! chatMessage
      clientConnection.expectMsg(200.millis, Write(ClientConnectionHandler.missingDestinationReply))
    }

    "let the sender know that the destination is not online" in {
      destinationClientIdentity = new ClientIdentity("Mr. AFK")
      val destination = Option[ClientIdentity](destinationClientIdentity)
      val source = Option[ClientIdentity](sourceClientIdentity)
      chatMessage = new ChatMessage(source, destination, payload)

      responseBroadcaster ! chatMessage
      clientConnection.expectMsg(200.millis,
        Write(ClientConnectionHandler.unresolvedDestinationReply(destinationClientIdentity.identity)))
    }

    "be able to broadcast a formatted response to its destination" in {
      responseBroadcaster ! chatMessage
      destinationConnection.expectMsgType[Write](200.millis)
      clientConnection.expectMsgType[Write](200.millis)
    }

  }

}
