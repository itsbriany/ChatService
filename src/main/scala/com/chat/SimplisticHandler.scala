package com.chat

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Received, Write}

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class SimplisticHandler extends Actor {
  def receive = {
    case Received(data) => sender() ! Write(data)
    case PeerClosed => context stop self
  }
}
