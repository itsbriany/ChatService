package com.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}

/**
  * Created by Brian.Yip on 8/26/2016.
  */
class Server extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", 9000))

  def receive = {
    case b@Bound(localAddress) => log.info(s"TCP server bound on ${localAddress.toString}")

    case CommandFailed(_: Bind) => context stop self

    case c@Connected(remote, local) =>
      log.info(s"Got a connection from ${remote.toString}")
      val connection = sender()
      val handler = context.actorOf(Props(new ClientConnectionHandler(connection)))
      connection ! Register(handler)
  }

}
