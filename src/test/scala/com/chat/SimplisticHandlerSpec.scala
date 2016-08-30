package com.chat

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by itsbriany on 2016-08-29.
  */
class SimplisticHandlerSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers {

  "A SimplisticHandler" must {
    val client = TestProbe()
    val simplisticHandler = TestActorRef(new SimplisticHandler(client.ref))

    "receive data from the client and reply to it" in {
      val message = "Hello"
      client.ref.tell(message, simplisticHandler)
      client.expectMsg(200.millis, message)
    }

    "proxy string data to the client" in {
      val message = "Some text"
      simplisticHandler ! message
      client.expectMsg(200.millis, message)
    }
  }
}
