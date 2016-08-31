package com.chat

import akka.actor.ActorSystem
import akka.io.Tcp.Write
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
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


  val identity = "My Identity"

  var clientConnection = TestProbe()
  var clientIdentity = new ClientIdentity(identity, clientConnection.ref)
  var clientIdentityResolver = TestActorRef(new ClientIdentityResolver())

  var addClientIdentity = new AddClientIdentity(clientIdentity)
  var removeClientIdentity = new RemoveClientIdentity(clientIdentity)
  var findClientIdentity = new FindClientIdentity(clientIdentity)

  override def beforeEach(): Unit = {
    clientConnection = TestProbe()
    clientIdentity = new ClientIdentity(identity, clientConnection.ref)
    addClientIdentity = new AddClientIdentity(clientIdentity)
    removeClientIdentity = new RemoveClientIdentity(clientIdentity)
    findClientIdentity = new FindClientIdentity(clientIdentity)
    clientIdentityResolver = TestActorRef(new ClientIdentityResolver())
  }

  s"A ${ClientIdentityResolver.getClass.getSimpleName}" must {

    s"map client identities to their ${ClientConnectionHandler.getClass.getSimpleName}" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver.underlyingActor.clientIdentityMap.get(identity) shouldBe Some(clientConnection.ref)
    }

    "greet the client when its identity was successfully added" in {
      clientIdentityResolver ! addClientIdentity
      clientConnection.expectMsg(200.millis, Write(ClientIdentityResolver.greeting(clientIdentity.getIdentity)))
    }

    "let its client know that there cannot be duplicate identities" in {
      clientIdentityResolver.underlyingActor.clientIdentityMap +=
        clientIdentity.getIdentity -> clientIdentity.getConnection

      clientIdentityResolver ! addClientIdentity

      clientConnection.expectMsg(200.millis, Write(ClientIdentityResolver.identityAlreadyExistsMessage(clientIdentity)))
    }

    "be capable of removing client identities and addresses" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver ! removeClientIdentity

      clientIdentityResolver.underlyingActor.clientIdentityMap.get(identity) shouldBe None
    }

    s"reply with a ${ClientConnectionHandler.getClass.getSimpleName} actorRef associated to an identity" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver ! findClientIdentity

      expectMsg(200.millis, clientConnection.ref)
    }

    "reply with None when an identity cannot be associated with an address" in {
      clientIdentityResolver ! findClientIdentity

      expectMsg(200.millis, None)
    }

  }

}
