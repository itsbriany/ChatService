package com.chat.message

import akka.actor.ActorRef

/**
  * Created by itsbriany on 2016-08-29.
  */
class ClientIdentity(identity: String, connection: ActorRef) {
  def getIdentity: String = identity

  def getConnection: ActorRef = connection

  def isIdentityEmpty: Boolean = identity.isEmpty
}
