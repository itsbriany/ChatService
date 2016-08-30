package com.chat.message

/**
  * Created by itsbriany on 2016-08-30.
  */
abstract class ClientIdentityModifier(clientIdentity: ClientIdentity) {
  def getClientIdentity: ClientIdentity = clientIdentity
}

class AddClientIdentity(clientIdentity: ClientIdentity) extends ClientIdentityModifier(clientIdentity) {}

class RemoveClientIdentity(clientIdentity: ClientIdentity) extends ClientIdentityModifier(clientIdentity) {}

class FindClientIdentity(clientIdentity: ClientIdentity) extends ClientIdentityModifier(clientIdentity) {}
