package org.scalaide.worksheet.runtime

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import scala.actors.{ Actor, DaemonActor }
import scala.sys.process.BasicIO
import scala.sys.process.Process
import scala.sys.process.ProcessIO
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.logging.HasLogger
import scala.util.{ Failure, Success }

import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.jdt.launching.JavaRuntime
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.editor.EditorProxy

object WorksheetRunner {

  private[runtime] def apply(scalaProject: ScalaProject): Actor = {
    val worksheet = new WorksheetRunner(scalaProject)
    worksheet.start()
    worksheet
  }

  trait Msg
  case class RunEvaluation(unit: ScriptCompilationUnit, editor: EditorProxy) extends Msg
}

/** An evaluator for worksheet documents.
 *
 *  It evaluates the contents of the given document and returns the output of the
 *  instrumented program.
 *
 *  It instantiates the instrumented program in-process, using a different class-loader.
 *  A more advanced evaluator would spawn a new VM, to allow debugging in the future.
 *
 */
private class WorksheetRunner private (scalaProject: ScalaProject) extends DaemonActor with HasLogger {
  import WorksheetRunner._
  import ResidentCompiler._

  private val config = Configuration(scalaProject)
  private val instrumenter = new SourceInstrumenter(config)
  private val compiler = ResidentCompiler(scalaProject, config)
  private val runner = ProgramExecutorService()

  override def act() = {
    loop {
      react {
        case RunEvaluation(unit, editor) =>
          unit.clearBuildErrors()
          instrumenter.instrument(unit) match {
            case Failure(ex) => eclipseLog.error(ex)
            case Success((decl, source)) =>
              compiler.compile(source) match {
                case CompilationFailed(errors) =>
                  logger.debug("compilation errors in " + (unit.file.name))
                  reportBuildErrors(unit, errors)

                case CompilationSuccess =>
                  runner ! ProgramExecutorService.RunProgram(unit, decl.fullName, classpath, editor)
              }
          }

        case msg: ProgramExecutorService.StopRun =>
          logger.info("forwarding " + msg + " to " + runner)
          runner forward msg

        case any => exit("Unsupported message " + any)
      }
    }
  }

  private def classpath: Seq[File] = {
    (scalaProject.scalaClasspath.fullClasspath ++ scalaProject.outputFolderLocations.map(_.toFile) :+ config.binFolder)
  }

  private def reportBuildErrors(unit: ScriptCompilationUnit, errors: Iterable[CompilationError]): Unit = {
    errors map { error => unit.reportBuildError(error.msg, error.pos) }
  }

  override def toString: String = "WorksheetEvaluator <actor>"
}