package com.chat

import akka.actor.ActorSystem
import akka.io.Tcp.Write
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by itsbriany on 2016-08-29.
  */
class ClientConnectionHandlerSpec extends TestKit(ActorSystem())
  with WordSpecLike
  with Matchers
  with BeforeAndAfterEach {

  var client = TestProbe()
  var clientConnectionHandler = TestActorRef(new ClientConnectionHandler(client.ref))

  override def beforeEach(): Unit = {
    client = TestProbe()
    clientConnectionHandler = TestActorRef(new ClientConnectionHandler(client.ref))
  }

  "A ClientConnectionHandler" must {
    "receive data from the client and reply to it when it has an identity" in {
      val identity = new ClientIdentity("Brian")
      clientConnectionHandler.tell(identity, client.ref)
      client.expectMsg(200.millis, Write(ClientConnectionHandler.greeting(identity.getIdentity)))

      val stringMessage = "Hello!"
      clientConnectionHandler.tell(stringMessage, client.ref)
      client.expectMsg(200.millis, Write(ByteString(stringMessage)))
    }

    "let the client know that they need to specify an identity when none is provided" in {
      val stringMessage = "Hello!"
      clientConnectionHandler.tell(stringMessage, client.ref)
      client.expectMsg(200.millis, Write(ClientConnectionHandler.missingIdentityReply))
    }
  }
}
