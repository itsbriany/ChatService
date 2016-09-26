package com.chat

import java.text.SimpleDateFormat
import java.util.Calendar

import GameEngine.Common.chat.{ChatMessage, ClientIdentity}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}

/**
  * Created by itsbriany on 2016-09-22.
  */
class ResponseBroadcasterSpec extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterEach {

  var responseBroadcaster = TestActorRef(new ResponseBroadcaster)
  var clientConnection = TestProbe()
  var destinationConnection = TestProbe()

  override def beforeEach(): Unit = {
    responseBroadcaster = TestActorRef(new ResponseBroadcaster)
    clientConnection = TestProbe()
    destinationConnection = TestProbe()
  }

  s"A ResponseBroadcaster" must {

    "format responses from chat messages before broadcasting" in {
      val sourceClientIdentity = new ClientIdentity("Source")
      val destinationClientIdentity = new ClientIdentity("Destination")
      val payload = "Some text message"
      val chatMessage =
        new ChatMessage(Option[ClientIdentity](sourceClientIdentity), Option[ClientIdentity](destinationClientIdentity), payload)

      val formatter = new SimpleDateFormat("HH:mm:ss")
      val timestamp = Calendar.getInstance().getTime
      val response = "[" + formatter.format(timestamp) + " Source] Some text message"
      val expected = ByteString(response)
      responseBroadcaster.underlyingActor.formatResponse(chatMessage, timestamp) shouldBe expected
    }

  }

}
