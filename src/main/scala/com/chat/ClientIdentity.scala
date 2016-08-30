package com.chat

/**
  * Created by itsbriany on 2016-08-29.
  */
// TODO: Move this to protobuf
class ClientIdentity(identity: String) {
  def getIdentity: String = {
    identity
  }

  def isEmpty: Boolean = {
    identity.isEmpty
  }
}
