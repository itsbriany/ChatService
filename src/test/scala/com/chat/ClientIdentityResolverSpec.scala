package com.chat

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.chat.message.{AddClientIdentity, ClientIdentity, FindClientIdentity, RemoveClientIdentity}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by itsbriany on 2016-08-30.
  */
class ClientIdentityResolverSpec extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterEach {

  val address = new InetSocketAddress("0.0.0.0", 0)
  val identity = "My Identity"
  val clientIdentity = new ClientIdentity(identity, address)
  val addClientIdentity = new AddClientIdentity(clientIdentity)
  val removeClientIdentity = new RemoveClientIdentity(clientIdentity)
  val findClientIdentity = new FindClientIdentity(clientIdentity)
  var clientIdentityResolver = TestActorRef(new ClientIdentityResolver())

  override def beforeEach(): Unit = {
    clientIdentityResolver = TestActorRef(new ClientIdentityResolver())
  }

  s"A ${ClientIdentityResolver.getClass.getSimpleName}" must {

    "map client identities to their addresses" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver.underlyingActor.clientIdentityMap.get(identity) shouldBe Some(address)
    }

    "let its sender know that there cannot be duplicate identities" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver ! addClientIdentity

      expectMsg(200.millis, ClientIdentityResolver.identityAlreadyExistsMessage(clientIdentity))
    }

    "be capable of removing client identities and addresses" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver ! removeClientIdentity

      clientIdentityResolver.underlyingActor.clientIdentityMap.get(identity) shouldBe None
    }

    "reply with an address associated to an identity" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver ! findClientIdentity

      expectMsg(200.millis, address)
    }

    "reply with None when an identity cannot be associated with an address" in {
      clientIdentityResolver ! findClientIdentity

      expectMsg(200.millis, None)
    }

  }

}
