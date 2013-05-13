package org.scalaide.worksheet.runtime

import java.io.StringWriter
import java.io.Writer
import java.util.concurrent.atomic.AtomicReference
import scala.actors.{ Actor, DaemonActor }
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.debug.core.DebugEvent
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.IDebugEventSetListener
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchManager
import org.eclipse.debug.core.IStreamListener
import org.eclipse.debug.core.Launch
import org.eclipse.debug.core.model.IFlushableStreamMonitor
import org.eclipse.debug.core.model.IProcess
import org.eclipse.debug.core.model.IStreamMonitor
import org.eclipse.jdt.launching.JavaRuntime
import org.eclipse.jdt.launching.VMRunnerConfiguration
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.widgets.Display
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.editor.DocumentHolder
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.properties.WorksheetPreferences

object ProgramExecutor {
  def apply(): Actor = {
    val executor = new ProgramExecutor
    executor.start()
    executor
  }

  case class RunProgram(unit: ScriptCompilationUnit, mainClass: String, classPath: Seq[String], editor: DocumentHolder)
  case object FinishedRun
  case class StopRun(unit: ScriptCompilationUnit) {
    def getId: String = getUnitId(unit)
  }

  private def getUnitId(unit: ScriptCompilationUnit): String = unit.file.file.getAbsolutePath()

  /** Return the process for this launch, if available. */
  private def getFirstProcess(launch: ILaunch): Option[IProcess] = launch.getProcesses().headOption

  /** Transfer data for the stream (out and error) of an IProcess to the mixer buffer. */
  private class StreamListener(writer: Writer) extends IStreamListener {
    def streamAppended(text: String, monitor: IStreamMonitor) {
      writer.write(text)
    }
  }

  /** Extractor of debug events. */
  private object EclipseDebugEvent {
    def unapply(event: DebugEvent): Option[(Int, Object)] = {
      event.getSource match {
        case element: Object =>
          Some(event.getKind, element)
        case _ =>
          None
      }
    }
  }

  /**
   *  Listen to Process terminate event. <p>
   *  Display non-modal error dialog upon abnormal termination, and
   *  require clean up of the evaluation executor.
   */
  private class DebugEventListener(service: ProgramExecutor, launchRef: AtomicReference[ILaunch],
      terminalMessage: => String) extends IDebugEventSetListener {
    // from org.eclipse.debug.core.IDebugEventSetListener
    override def handleDebugEvents(debugEvents: Array[DebugEvent]) {
      debugEvents foreach {
        _ match {
          case EclipseDebugEvent(DebugEvent.TERMINATE, element) =>
            if (Option(element) == getFirstProcess(launchRef.get)) {
              val process = getFirstProcess(launchRef.get).get
              if (process.getExitValue()!= 0) {
                Display.getDefault().asyncExec(new Runnable() {
                  def run() {
                    val stderr = process.getStreamsProxy().getErrorStreamMonitor().getContents()
                    val shell = Display.getCurrent().getActiveShell()
                    MessageDialog.openError(shell, "Worksheet terminated unexpectedly",
                      "Worksheet process has terminated unexpectedly (exit value "+process.getExitValue()+")\n" +
                      "At the time of termination, the following text was available in the output streams:\n\n" +
                      "Standard output:\n"+terminalMessage+"\n" +
                      "Standard error: \n"+stderr)
                  }
                })
              }
              service ! FinishedRun
            }
          case _ =>
        }
      }
    }
  }
}

private class ProgramExecutor private () extends DaemonActor with HasLogger {
  import ProgramExecutor._
  import scala.actors.{ AbstractActor, Exit }

  private var id: String = _
  private val launchRef: AtomicReference[ILaunch] = new AtomicReference

  private var editorUpdater: Actor = _
  private var debugEventListener: IDebugEventSetListener = _

  override def toString(): String = "ProgramExecutor actor <" + id + ">"

  override def act(): Unit = {
    loop {
      react {
        case RunProgram(unit, mainClass, cp, doc) =>
          trapExit = true // get notified if a slave actor fails

          id = getUnitId(unit)

          // Get the vm configured to the project, and a runner to launch an process
          val vmInstall = JavaRuntime.getVMInstall(unit.scalaProject.javaProject)
          val vmRunner = vmInstall.getVMRunner(ILaunchManager.RUN_MODE)

          // simple configuration, main class and classpath
          val vmRunnerConfig = new VMRunnerConfiguration(mainClass, cp.toArray)
          vmRunnerConfig.setVMArguments(Array("-Dfile.encoding="+doc.encoding.name()))

          // obtain and assign VM arguments, split by whitespace
          // empty string arguments are eliminated, since the jvm will regard one as a main class argument.
          val vmArgs = WorksheetPlugin.plugin.getPreferenceStore().getString(WorksheetPreferences.P_VM_ARGS)
            .split("""\s""").filterNot(_.isEmpty)
          vmRunnerConfig.setVMArguments(vmArgs)

          // a launch is need to get the created process
          val launch: ILaunch = new Launch(null, null, null)
          launchRef.set(launch)

          // StringWriter instance for buffering consumption from stdout
          val stdoutWriter = new StringWriter()

          // assemble message upon abnormal termination of worksheet process
          def terminalMessage = {
            val stdoutStream = getFirstProcess(launch).get.getStreamsProxy().getOutputStreamMonitor()
            stdoutStream.synchronized {
              stdoutWriter.write(stdoutStream.getContents())
              stdoutStream match {
                case flushableStream: IFlushableStreamMonitor =>
                  flushableStream.flushContents()
                case _ =>
              }
              stdoutWriter.toString()
            }
          }


          // listener to know when the process terminates
          debugEventListener = new DebugEventListener(ProgramExecutor.this, launchRef, terminalMessage)
          DebugPlugin.getDefault().addDebugEventListener(debugEventListener)

          // launch the vm
          vmRunner.run(vmRunnerConfig, launch, new NullProgressMonitor)

          // We have a process at this point
          val process = getFirstProcess(launch).get

          // connect the out and error streams to the buffer used by the mixer
          connectListener(process.getStreamsProxy().getOutputStreamMonitor(), stdoutWriter)

          val maxOutput = WorksheetPlugin.plugin.getPreferenceStore().getInt(WorksheetPreferences.P_CUTOFF_VALUE)
          // start the mixer
          editorUpdater = IncrementalDocumentMixer(doc, stdoutWriter, maxOutput)
          link(editorUpdater)

          // switch behavior. Waits for the end or the interruption of the evaluation
          running()

        case msg @ StopRun(unitId) => ignoreStopRun(msg)
        case msg: Exit             => resetStateOnSlaveFailure(msg)
        case any                   => logger.debug(ProgramExecutor.this.toString() + ": swallow message " + any)
      }
    }
  }

  private def running(): Unit = react {
    case msg: StopRun => if (msg.getId == id) reset() else ignoreStopRun(msg)
    case FinishedRun  => reset()
    case msg: Exit    => resetStateOnSlaveFailure(msg)
  }

  private def resetStateOnSlaveFailure(exit: Exit): Unit = {
    logger.debug(exit.from.toString + " unexpectedly terminated, reason: " + exit.reason + ". " + "Resetting state of " + ProgramExecutor.this
      + " and getting ready to process new requests.")
    reset()
  }

  private def ignoreStopRun(msg: StopRun): Unit = {
    logger.info("Ignoring " + msg + ": we're executing: " + id)
  }

  private def reset(): Unit = {
    // While we shut down the slave actors, we are not interested in listening
    // to termination events (which could be triggered by the below actions)
    trapExit = false

    if (editorUpdater != null) {
      unlink(editorUpdater)
      editorUpdater ! 'stop
      editorUpdater = null
    }

    if (launchRef.get != null) {
      getFirstProcess(launchRef.get) foreach { _.terminate() }
      launchRef.set(null)
    }

    if (debugEventListener != null) {
      DebugPlugin.getDefault().removeDebugEventListener(debugEventListener)
      debugEventListener = null
    }

    id = null
  }

  override def exceptionHandler: PartialFunction[Exception, Unit] = {
    case e: Exception =>
      eclipseLog.warn(ProgramExecutor.this.toString + " self-healing...", e)
      reset()
  }

  /** Connect an IProcess stream to the writer.*/
  private def connectListener(stream: IStreamMonitor, writer: Writer) {
    val listener = new StreamListener(writer)

    // there is some possible race condition between getting the already current content
    // and getting update through the listener. IFlushableStreamMonitor has a solution for it.
    stream match {
      case flushableStream: IFlushableStreamMonitor =>
        flushableStream.synchronized {
          flushableStream.addListener(listener)
          writer.write(flushableStream.getContents())
          flushableStream.flushContents()
          flushableStream.setBuffered(false)
        }
      case otherStream =>
        // it is unlikely to not have an IFlushableStreamMonitor
        otherStream.addListener(listener)
        listener.streamAppended(otherStream.getContents(), otherStream)
    }
  }
}
