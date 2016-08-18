package com.example

import com.example.actors.HelloWorldActor

object Hello {

  def main(args: Array[String]): Unit = {
    val initialActor = classOf[HelloWorldActor].getName

    akka.Main.main(Array(initialActor))
  }

}
