package org.scalaide.worksheet.runtime

import scala.collection.concurrent
import scala.ref.WeakReference
import scala.util.Try

import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IPath
import org.scalaide.logging.HasLogger
import org.scalaide.worksheet.ScriptCompilationUnit

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object WorksheetsManager {
  final val ActorName = "worksheet-manager"
  def props(): Props = Props(new WorksheetsManager)

  private object ProjectChangeListener {
    def apply(system: ActorSystem, worksheetRunners: concurrent.Map[IPath, ActorRef]): IResourceChangeListener =
      new ProjectChangeListener(WeakReference(system), WeakReference(worksheetRunners))

    private class ProjectChangeListener(system: WeakReference[ActorSystem], worksheetRunners: WeakReference[concurrent.Map[IPath, ActorRef]]) extends IResourceChangeListener {
      import IResourceChangeEvent._
      def resourceChanged(event: IResourceChangeEvent): Unit = {
        def resource = event.getResource()
        event.getType match {
          case PRE_CLOSE  => removeWorksheetRunnerFor(resource.getFullPath)
          case PRE_DELETE => removeWorksheetRunnerFor(resource.getFullPath)
        }
      }

      private def removeWorksheetRunnerFor(fullpath: IPath): Unit = {
        for {
          worksheetRunners <- worksheetRunners.get
          worksheetRunner <- worksheetRunners.remove(fullpath)
        } system.get.foreach(_.stop(worksheetRunner))
      }
    }
  }
}

private class WorksheetsManager private extends Actor with HasLogger {
  import WorksheetsManager.ProjectChangeListener
  import WorksheetRunner.RunEvaluation
  import ProgramExecutor.StopRun

  private val worksheets: concurrent.Map[IPath, ActorRef] = concurrent.TrieMap.empty
  private var projectListener: IResourceChangeListener = _

  override def preStart(): Unit = {
    projectListener = ProjectChangeListener(context.system, worksheets)
    workspace.addResourceChangeListener(projectListener, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE)
    super.preStart()
  }

  override def postStop(): Unit = {
    workspace.removeResourceChangeListener(projectListener)
    projectListener = null
    super.postStop()
  }

  private def workspace = ResourcesPlugin.getWorkspace()

  override def receive = {
    case msg @ RunEvaluation(unit, _) =>
      obtainEvaluator(unit) forward msg

    case msg: StopRun =>
      forwardIfEvaluatorExists(msg)
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
        val evaluator = context.actorOf(WorksheetRunner.props(scalaProject), "worksheet-runner-for-project-" + scalaProject.underlying.getName)
        worksheets += (scalaProject.underlying.getFullPath -> evaluator)
        evaluator
    }
  }

  override def toString: String = "WorksheetManager <actor>"
}