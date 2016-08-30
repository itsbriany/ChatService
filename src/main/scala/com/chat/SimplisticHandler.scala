package com.chat

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.{PeerClosed, Received, Write}

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class SimplisticHandler(connection: ActorRef) extends Actor {
  def receive = {
    case text: String => connection ! text
    case Received(data) => connection ! Write(data)
    case PeerClosed => context stop self
  }
}
