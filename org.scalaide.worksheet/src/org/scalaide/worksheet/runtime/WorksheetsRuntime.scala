package org.scalaide.worksheet.runtime

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.Props
import akka.actor.ActorRef

object WorksheetsRuntime {
  final val ActorName = "worksheet-runtime"

  def props(): Props = Props(new WorksheetsRuntime)
}

private class WorksheetsRuntime private() extends Actor {

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception => Resume
  }

  var worksheetsManager: ActorRef = _
  
  override def preStart(): Unit = {
    worksheetsManager = context.actorOf(WorksheetsManager.props(), WorksheetsManager.ActorName)
  }

  override def receive() = {
    case msg => worksheetsManager forward msg
  }
}