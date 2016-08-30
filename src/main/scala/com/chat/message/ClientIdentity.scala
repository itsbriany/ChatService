package com.chat.message

import java.net.InetSocketAddress

/**
  * Created by itsbriany on 2016-08-29.
  */
// TODO: Move this to protobuf
class ClientIdentity(identity: String, address: InetSocketAddress) {
  def getIdentity: String = identity

  def getAddress: InetSocketAddress = address

  def isEmpty: Boolean = identity.isEmpty
}
