package org.scalaide.worksheet.runtime
import scala.actors.{ Actor, DaemonActor }
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.logging.HasLogger
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.editor.DocumentHolder
import org.scalaide.worksheet.text.SourceInserter
import scala.tools.eclipse.BuildSuccessListener
import scala.tools.eclipse.util.SWTUtils

object WorksheetRunner {

  private[runtime] def apply(scalaProject: ScalaProject): Actor = {
    val worksheet = new WorksheetRunner(scalaProject)
    worksheet.start()
    worksheet
  }

  case class RunEvaluation(unit: ScriptCompilationUnit, editor: DocumentHolder)
  case object RefreshResidentCompiler
}

/** An evaluator for worksheet documents.
 *
 *  It evaluates the contents of the given document and returns the output of the
 *  instrumented program.
 *
 *  It instantiates the instrumented program in-process, using a different class-loader.
 *  A more advanced evaluator would spawn a new VM, to allow debugging in the future.
 */
private class WorksheetRunner private (scalaProject: ScalaProject) extends DaemonActor with HasLogger {
  import WorksheetRunner._
  import ResidentCompiler._

  private val config = Configuration(scalaProject)
  private val instrumenter = new SourceInstrumenter(config)
  private var compiler = ResidentCompiler(scalaProject, config)
  private val executor = ProgramExecutor()

  private object buildListener extends BuildSuccessListener {
    def buildSuccessful() {
      WorksheetRunner.this ! RefreshResidentCompiler
    }
  }

  scalaProject.addBuildSuccessListener(buildListener)

  override def act() = {
    loop {
      react {
        case RunEvaluation(unit, editor) =>
          unit.clearBuildErrors()

          val stripped = SourceInserter.stripRight(editor.getContents.toCharArray())
          editor.replaceWith(stripped.mkString)

          instrumenter.instrument(unit) match {
            case Left(ex) => eclipseLog.error("Error during instrumentation of " + unit, ex)
            case Right((decl, source)) =>
              compiler.compile(source) match {
                case CompilationFailed(errors) =>
                  logger.debug("compilation errors in " + (unit.file.name))
                  editor.endUpdate()
                  // Fix the race condition in error markers by updating them
                  // on the UI thread. Otherwise, the 'replaceWith' call before
                  // might remove all markers, considering their positions 'deleted' 
                  // by the replace action
                  SWTUtils.asyncExec { reportBuildErrors(unit, errors) }

                case CompilationSuccess =>
                  executor ! ProgramExecutor.RunProgram(unit, decl.fullName, classpath, editor)
              }
          }

        case msg: ProgramExecutor.StopRun =>
          logger.info("forwarding " + msg + " to " + executor)
          executor forward msg

        case RefreshResidentCompiler =>
          logger.info("Refreshing worksheet resident compiler for " + scalaProject)
          compiler = ResidentCompiler(scalaProject, config)

        case any =>
          scalaProject.removeBuildSuccessListener(buildListener)
          exit("Unsupported message " + any)
      }
    }
  }

  /** The classpath, as a list of local filesystem path
   */
  private def classpath: Seq[String] = {
    (scalaProject.scalaClasspath.fullClasspath.map(_.getAbsolutePath()) ++ scalaProject.outputFolderLocations.map(_.toOSString()) :+ config.binFolder.getAbsolutePath())
  }

  private def reportBuildErrors(unit: ScriptCompilationUnit, errors: Iterable[CompilationError]): Unit = {
    errors map { error => unit.reportBuildError(error.msg, error.pos) }
  }

  override def toString: String = "WorksheetEvaluator <actor>"
}