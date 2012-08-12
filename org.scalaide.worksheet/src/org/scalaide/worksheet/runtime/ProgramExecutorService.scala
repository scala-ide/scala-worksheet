package org.scalaide.worksheet.runtime


import java.util.concurrent.atomic.AtomicReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import scala.actors.{ Actor, DaemonActor }
import scala.sys.process.BasicIO
import scala.sys.process.Process
import scala.sys.process.ProcessIO
import scala.sys.process.ProcessBuilder
import scala.tools.eclipse.logging.HasLogger

import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.editor.EditorProxy

import org.scalaide.worksheet.util.using

object ProgramExecutorService {
  def apply(): Actor = {
    val executor = new ProgramExecutorService
    executor.start()
    executor
  }

  private object ScalaRunner {
    def apply(processRef: AtomicReference[Process]): Actor = {
      val executor = new ScalaRunner(processRef)
      executor.start()
      executor
    }

    case class RunScalaApp(mainClass: String)
  }

  private class ScalaRunner private (processRef: AtomicReference[Process]) extends Actor with HasLogger {
    import ScalaRunner.RunScalaApp
    override def act() = react {
      case RunScalaApp(mainClass) =>
        val process = processRef.get
        if (process != null) {
          val exitCode = process.exitValue()
          logger.debug("Evaluation completed with exit code: %d".format(exitCode))
        }
        reply(FinishedRun)

      case any => exit(this.toString + ": Unsupported message " + any)
    }
  }

  object RunProgram {
    def apply(unit: ScriptCompilationUnit, mainClass: String, classPath: Seq[File], editor: EditorProxy): RunProgram =
      RunProgram(getUnitId(unit), mainClass, classPath, editor)
  }
  case class RunProgram private (unitId: String, mainClass: String, classPath: Seq[File], editor: EditorProxy)
  case object FinishedRun
  case class StopRun(unit: ScriptCompilationUnit) {
    def getId: String = getUnitId(unit)
  }

  private def getUnitId(unit: ScriptCompilationUnit): String = unit.file.file.getAbsolutePath()
}

private class ProgramExecutorService private () extends DaemonActor with HasLogger {
  import ProgramExecutorService.{ RunProgram, FinishedRun, StopRun }
  import scala.actors.{ AbstractActor, Exit }

  private var id: String = _
  private val processRef: AtomicReference[Process] = new AtomicReference

  private var editorUpdater: Actor = _
  private var runner: Actor = _

  override def act(): Unit = {
    loop {
      react {
        case RunProgram(unitId, mainClass, cp, doc) =>
          trapExit = true // get notified if a slave actor fails

          /** Launch `mainClass` in a different JVM and return everything on stdout and stderr.
           *
           *  This implementation uses `scala.sys.process` to launch `java`, which needs to be
           *  on the classpath. It adds the project classpath and its output folders on the VM
           *  classpath.
           */
          id = unitId
          val baos = new ByteArrayOutputStream
          editorUpdater = IncrementalDocumentMixer(doc, baos)
          link(editorUpdater)

          lazy val outStream = new PrintStream(baos)
          val pio = new ProcessIO(in => (),
            os => using(outStream)(BasicIO.transferFully(os, _)),
            es => using(outStream)(BasicIO.transferFully(es, _)),
            true)

          val rawCp = cp.map(_.getAbsolutePath()).mkString("", File.pathSeparator, "")
          val javaCmd = List("java", "-cp") :+ rawCp :+ mainClass

          logger.debug("Running " + javaCmd.mkString("", " ", ""))
          val builder = Process(javaCmd.toArray)
          processRef.set(builder.run(pio))
          runner = ProgramExecutorService.ScalaRunner(processRef)
          link(runner)

          runner ! ProgramExecutorService.ScalaRunner.RunScalaApp(mainClass)
          running()

        case msg @ StopRun(unitId) => ignoreStopRun(msg)
        case msg: Exit             => resetStateOnSlaveFailure(msg)
        case any => logger.debug(this.toString + ": swallow message " + any)
      }
    }
  }

  private def running() = react {
    case msg: StopRun => if (msg.getId == id) reset() else ignoreStopRun(msg)
    case FinishedRun  => reset()
    case msg: Exit    => resetStateOnSlaveFailure(msg)
  }

  private def resetStateOnSlaveFailure(exit: Exit): Unit = {
    logger.debug(exit.from.toString + " unexpectedly terminated, reason: " + exit.reason + ". " + "Resetting state of " + this
      + " and getting ready to process new requests.")
    reset()
  }

  private def ignoreStopRun(msg: StopRun): Unit = {
    logger.info("Ignoring " + msg + ": no program is currently being executed.")
  }

  private def reset(): Unit = {
    // While we shut down the slave actors, we are not interested in listening 
    // to termination events (which could be triggered by the below actions)  
    trapExit = false

    if (runner != null) {
      unlink(runner)
      runner = null
    }

    if (editorUpdater != null) {
      unlink(editorUpdater)
      editorUpdater ! 'stop
      editorUpdater = null
    }

    if (processRef.get != null) {
      processRef.get.destroy()
      processRef.set(null)
    }

    id = null
  }

  override def exceptionHandler: PartialFunction[Exception, Unit] = {
    case e: Exception =>
      eclipseLog.warn(this.toString + " self-healing...", e)
      reset()
  }

  override def toString: String = "ProgramExecutorService <actor>"

  //  val manager = DebugPlugin.getDefault().getLaunchManager()
  //
  //  private def getLaunchConfig(): ILaunchConfigurationWorkingCopy = {
  //
  //    val confType = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION)
  //    for {
  //      conf <- manager.getLaunchConfigurations(confType)
  //      if conf.getName() == WORKSHEET_LAUNCH_CONFIGURATION
  //    } conf.delete()
  //
  //    confType.newInstance(null, WORKSHEET_LAUNCH_CONFIGURATION)
  //  }
  //
  //  private def getVMInstall() {
  //
  //    val jre = JavaRuntime.getDefaultVMInstall()
  //
  //  }
  //
  //  final val WORKSHEET_LAUNCH_CONFIGURATION = "Start Worksheet"
}