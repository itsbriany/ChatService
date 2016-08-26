package com.chat

import akka.actor.{ActorSystem, Props}

/**
  * Created by Brian.Yip on 8/26/2016.
  */
object ChatSystem extends App {
  implicit val system = ActorSystem("ChatService")
  system.actorOf(Props[Server], "Server")
}
