package org.scalaide.worksheet.runtime

import scala.actors.{ Actor, DaemonActor }
import scala.collection.mutable
import scala.tools.eclipse.ScalaProject
import org.scalaide.worksheet.ScriptCompilationUnit

object WorksheetsManager {
  lazy val Instance: Actor = {
    val proxy = new WorksheetsManager
    proxy.start()
    proxy
  }
}

private class WorksheetsManager private extends DaemonActor {
  import WorksheetRunner.RunEvaluation
  import ProgramExecutorService.StopRun

  //FIXME: Need a way to dispose worksheet evaluator and remove it from the map when the project is disposed (listener!?)
  private val scalaProject2worksheetEvaluator: mutable.Map[ScalaProject, Actor] = new mutable.HashMap

  override def act() = loop {
    react {
      case msg @ RunEvaluation(unit, _) =>
        obtainEvaluator(unit) forward msg

      case msg: StopRun =>
        forwardIfEvaluatorExists(msg)

      case any => exit("Unsupported message " + any)
    }
  }

  private def forwardIfEvaluatorExists(msg: StopRun): Unit = {
    val scalaProject = msg.unit.scalaProject
    for (evaluator <- scalaProject2worksheetEvaluator.get(scalaProject))
      evaluator forward msg
  }

  private def obtainEvaluator(unit: ScriptCompilationUnit): Actor = {
    val scalaProject = unit.scalaProject
    scalaProject2worksheetEvaluator.get(scalaProject) match {
      case Some(evaluator) => evaluator
      case None =>
        val evaluator = WorksheetRunner(scalaProject)
        scalaProject2worksheetEvaluator += (scalaProject -> evaluator)
        evaluator
    }
  }

  override def toString: String = "WorksheetManager <actor>"
}