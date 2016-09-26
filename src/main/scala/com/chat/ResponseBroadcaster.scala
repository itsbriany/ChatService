package com.chat

import java.text.SimpleDateFormat
import java.util.Date

import GameEngine.Common.chat.ChatMessage
import akka.actor.{Actor, ActorLogging}
import akka.io.Tcp.Write
import akka.util.ByteString
import com.chat.message.BroadcastedResponse

/**
  * Created by itsbriany on 2016-09-22.
  */
class ResponseBroadcaster extends Actor with ActorLogging {
  override def receive: Receive = {
    case broadcastedResponse: BroadcastedResponse => handleBroadcastedResponse(broadcastedResponse)
  }

  def handleBroadcastedResponse(broadcastedResponse: BroadcastedResponse): Unit = {
    val response = ByteString(broadcastedResponse.getChatMessage.text)

    broadcastedResponse.getSource ! Write(response)
    if (broadcastedResponse.getSource != broadcastedResponse.getDestination)
      broadcastedResponse.getDestination ! Write(response)
  }

  def formatResponse(chatMessage: ChatMessage, timestamp: Date): ByteString = {
    val formatter = new SimpleDateFormat("HH:mm:ss")
    val timestampAsString = formatter.format(timestamp)

    // [DateFormat Source] Message
    val buffer = new StringBuilder
    buffer += '['
    buffer ++= timestampAsString
    buffer += ' '
    buffer ++= chatMessage.getSource.identity
    buffer ++= "] "
    buffer ++= chatMessage.text

    ByteString(buffer.toString())
  }
}
