package org.scalaide.worksheet.runtime

import scala.actors.{ Actor, DaemonActor }
import scala.collection.immutable
import scala.tools.eclipse.ScalaProject
import org.scalaide.worksheet.ScriptCompilationUnit
import scala.actors.Exit
import scala.tools.eclipse.logging.HasLogger
import scala.actors.AbstractActor
import org.eclipse.core.runtime.IPath

object WorksheetsManager {
  lazy val Instance: Actor = {
    val proxy = new WorksheetsManager
    proxy.start()
    proxy
  }
}

private class WorksheetsManager private extends DaemonActor with HasLogger {
  import WorksheetRunner.RunEvaluation
  import ProgramExecutor.StopRun

  //FIXME: Need a way to dispose worksheet evaluator and remove it from the map when the project is disposed (listener!?)
  private var worksheetEvaluator: immutable.Map[IPath, Actor] = new immutable.HashMap

  override def act() = loop {
    react {
      case msg @ RunEvaluation(unit, _) =>
        obtainEvaluator(unit) forward msg

      case msg: StopRun =>
        forwardIfEvaluatorExists(msg)

      case Exit(actor, reason) =>
        eclipseLog.error("Evaluator actor crashed " + reason)
        evictEvaluator(actor)

      case any => exit("Unsupported message " + any)
    }
  }

  private def evictEvaluator(actor: AbstractActor) {
    worksheetEvaluator = worksheetEvaluator filterNot { case (_, a) => a == actor }
  }

  private def forwardIfEvaluatorExists(msg: StopRun): Unit = {
    val scalaProject = msg.unit.scalaProject
    for (evaluator <- worksheetEvaluator.get(scalaProject.underlying.getFullPath))
      evaluator forward msg
  }

  private def obtainEvaluator(unit: ScriptCompilationUnit): Actor = {
    val scalaProject = unit.scalaProject
    worksheetEvaluator.get(scalaProject.underlying.getFullPath) match {
      case Some(evaluator) => evaluator
      case None =>
        val evaluator = WorksheetRunner(scalaProject)
        trapExit = true
        link(evaluator)

        worksheetEvaluator = worksheetEvaluator + (scalaProject.underlying.getFullPath -> evaluator)
        evaluator
    }
  }

  override def toString: String = "WorksheetManager <actor>"
}