package com.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem}
import akka.io.Tcp.Connect
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class ChatServerSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers {

  class TCPTestClient extends Actor {
    override def receive: Receive = {
      case value: String =>
        IO(Tcp) ! Connect(new InetSocketAddress("0.0.0.0", 9000))
        sender() ! "Connected!"
    }
  }

  s"A ChatServer" must {
    val server = TestActorRef(new ChatServer())

    "handle inbound TCP connections" in {
      implicit val timeout = Timeout(500.millis)
      val client = TestActorRef(new TCPTestClient())
      val response = client ? "make a connection"

      Await.result(response, 1.second)

      // It takes some time to create the child actor
      Thread.sleep(500)
      server.children.size shouldBe 1
    }
  }

}
