package com.chat.message

/**
  * Created by itsbriany on 2016-08-30.
  */
abstract class ActorClientModifier(clientIdentity: ActorClient) {
  def getClientIdentity: ActorClient = clientIdentity
}

class AddActorClient(client: ActorClient) extends ActorClientModifier(client) {}

class RemoveActorClient(client: ActorClient) extends ActorClientModifier(client) {}

class FindActorClient(client: ActorClient) extends ActorClientModifier(client) {}
