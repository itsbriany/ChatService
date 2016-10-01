package com.chat

import GameEngine.Common.chat.Identity
import akka.actor.ActorSystem
import akka.io.Tcp.Write
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.chat.message.{ActorClient, AddActorClient, FindActorClient, RemoveActorClient}
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


  val identity = new Identity("My Identity")

  var clientConnection = TestProbe()
  var actorClient = new ActorClient(identity, clientConnection.ref)
  var clientIdentityResolver = TestActorRef(new IdentityResolver())

  var addClientIdentity = new AddActorClient(actorClient)
  var removeClientIdentity = new RemoveActorClient(actorClient)
  var findClientIdentity = new FindActorClient(actorClient)

  override def beforeEach(): Unit = {
    clientConnection = TestProbe()
    actorClient = new ActorClient(identity, clientConnection.ref)
    addClientIdentity = new AddActorClient(actorClient)
    removeClientIdentity = new RemoveActorClient(actorClient)
    findClientIdentity = new FindActorClient(actorClient)
    clientIdentityResolver = TestActorRef(new IdentityResolver())
  }

  s"A ${IdentityResolver.getClass.getSimpleName}" must {

    s"map client identities to their ${ClientConnection.getClass.getSimpleName}" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver.underlyingActor.clientIdentityMap.get(identity.name) shouldBe Some(clientConnection.ref)
    }

    "greet the client when its identity was successfully added" in {
      clientIdentityResolver ! addClientIdentity
      clientConnection.expectMsg(200.millis, Write(IdentityResolver.greeting(actorClient.getIdentity.name)))
    }

    "let its client know that there cannot be duplicate identities" in {
      clientIdentityResolver.underlyingActor.clientIdentityMap +=
        actorClient.getIdentity.name -> actorClient.getActorRef

      clientIdentityResolver ! addClientIdentity

      clientConnection.expectMsg(200.millis, Write(IdentityResolver.identityAlreadyExistsMessage(actorClient)))
    }

    "be capable of removing client identities and addresses" in {
      clientIdentityResolver ! addClientIdentity
      clientIdentityResolver ! removeClientIdentity

      clientIdentityResolver.underlyingActor.clientIdentityMap.get(identity.name) shouldBe None
    }

    s"reply with a ${ClientConnection.getClass.getSimpleName} actorRef associated to an identity" in {
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
