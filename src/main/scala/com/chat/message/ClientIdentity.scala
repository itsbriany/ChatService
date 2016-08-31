package com.chat.message

import java.net.InetSocketAddress

import akka.actor.ActorRef

/**
  * Created by itsbriany on 2016-08-29.
  */
class ClientIdentity(identity: String, address: InetSocketAddress, connection: ActorRef) {
  def getIdentity: String = identity

  def getAddress: InetSocketAddress = address

  def getConnection: ActorRef = connection

  def isIdentityEmpty: Boolean = identity.isEmpty
}
