package com.chat.message

import GameEngine.Common.chat.Identity
import akka.actor.ActorRef

/**
  * Created by itsbriany on 2016-08-29.
  */
class ActorClient(identity: Identity, connection: ActorRef) {
  def getIdentity: Identity = identity

  def getActorRef: ActorRef = connection
}

class SetActorClient(actorClient: ActorClient) {}
