package org.scalaide.worksheet.runtime

import scala.collection.immutable
import scala.util.Try

import org.eclipse.core.runtime.IPath
import org.scalaide.logging.HasLogger
import org.scalaide.worksheet.ScriptCompilationUnit

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated

object WorksheetsManager {
  final val ActorName = "worksheet-manager"
  def props(): Props = Props(new WorksheetsManager)
}

private class WorksheetsManager private extends Actor with HasLogger {
  import WorksheetRunner.RunEvaluation
  import ProgramExecutor.StopRun

  //FIXME: Need to dispose worksheet evaluator and remove it from the map when the project is disposed
  private var worksheets: immutable.Map[IPath, ActorRef] = immutable.HashMap.empty

  override def receive = {
      case msg @ RunEvaluation(unit, _) =>
        obtainEvaluator(unit) forward msg

      case msg: StopRun =>
        forwardIfEvaluatorExists(msg)

      case Terminated(actor) =>
        evictEvaluator(actor)
    }

  private def evictEvaluator(actor: ActorRef) {
    context.unwatch(actor)
    worksheets = worksheets filterNot { case (_, a) => a == actor }
  }

  private def forwardIfEvaluatorExists(msg: StopRun): Unit = {
    // if the project is closed, `unit.scalaProject` throws NoSuchElementException
    // TODO: ScalaPlugin.asScalaProject probably shouldn't filter out closed projects.
    for {
      scalaProject <- Try(msg.unit.scalaProject)
      evaluator <- worksheets.get(scalaProject.underlying.getFullPath)
    } evaluator forward msg
  }

  private def obtainEvaluator(unit: ScriptCompilationUnit): ActorRef = {
    val scalaProject = unit.scalaProject
    worksheets.get(scalaProject.underlying.getFullPath) match {
      case Some(evaluator) => evaluator
      case None =>
        val evaluator = context.actorOf(WorksheetRunner.props(scalaProject), "worksheet-runner-for-project-"+scalaProject.underlying.getName)
        context.watch(evaluator)

        worksheets += (scalaProject.underlying.getFullPath -> evaluator)
        evaluator
    }
  }

  override def toString: String = "WorksheetManager <actor>"
}