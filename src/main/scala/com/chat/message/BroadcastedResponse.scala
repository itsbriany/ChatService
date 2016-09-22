package com.chat.message

import GameEngine.Common.chat.ChatMessage
import akka.actor.ActorRef

/**
  * Created by itsbriany on 2016-09-22.
  */
class BroadcastedResponse(sourceConnectionHandler: ActorRef, destinationConnectionHandler: ActorRef, chatMessage: ChatMessage) {
  def getSource = sourceConnectionHandler

  def getDestination = destinationConnectionHandler

  def getChatMessage = chatMessage
}
