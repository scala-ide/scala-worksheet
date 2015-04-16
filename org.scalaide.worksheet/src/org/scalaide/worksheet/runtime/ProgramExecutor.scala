package org.scalaide.worksheet.runtime

import java.io.StringWriter
import java.io.Writer
import java.util.concurrent.atomic.AtomicReference

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
import org.scalaide.logging.HasLogger
import org.scalaide.util.ui.DisplayThread
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.editor.DocumentHolder
import org.scalaide.worksheet.properties.WorksheetPreferences

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._

object ProgramExecutor extends HasLogger {
  def props(): Props = Props(new ProgramExecutor)

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
    override def streamAppended(text: String, monitor: IStreamMonitor) {
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
  private class DebugEventListener(programExecutorActor: ActorRef, launchRef: AtomicReference[ILaunch],
      terminalMessage: => String) extends IDebugEventSetListener {
    // from org.eclipse.debug.core.IDebugEventSetListener
    override def handleDebugEvents(debugEvents: Array[DebugEvent]) {
      debugEvents foreach {
        _ match {
          case EclipseDebugEvent(DebugEvent.TERMINATE, element) =>
            if (Option(element) == getFirstProcess(launchRef.get)) {
              val process = getFirstProcess(launchRef.get).get
              if (process.getExitValue()!= 0) {
                val stderr = process.getStreamsProxy().getErrorStreamMonitor().getContents()
                val message = s"""|Worksheet process has terminated unexpectedly (exit value ${process.getExitValue()}
                                  |At the time of termination, the following text was available in the output streams:
                                  |Standard output:
                                  |  $terminalMessage
                                  |Standard error:
                                  |  $stderr
                                  |""".stripMargin
                logger.error(message)
                DisplayThread asyncExec {
                  val shell = Display.getCurrent().getActiveShell()
                  MessageDialog.openError(shell, "Worksheet terminated unexpectedly", message)
                }
              }
              programExecutorActor ! FinishedRun
            }
          case _ =>
        }
      }
    }
  }
}

private class ProgramExecutor private () extends Actor with HasLogger {
  import ProgramExecutor._

  private var currentRunId: String = _
  private val launchRef: AtomicReference[ILaunch] = new AtomicReference

  private var editorUpdater: ActorRef = _
  private var debugEventListener: IDebugEventSetListener = _

  override def toString(): String = "ProgramExecutor actor <" + currentRunId + ">"

  override val supervisorStrategy = OneForOneStrategy() {
    case _ => Stop
  }
  
  override def receive: Receive = {
        case RunProgram(unit, mainClass, cp, doc) =>
          currentRunId = getUnitId(unit)

          // Get the vm configured to the project, and a runner to launch an process
          val vmInstall = JavaRuntime.getVMInstall(unit.scalaProject.javaProject)
          val vmRunner = vmInstall.getVMRunner(ILaunchManager.RUN_MODE)

          // simple configuration, main class and classpath
          val vmRunnerConfig = new VMRunnerConfiguration(mainClass, cp.toArray)
          val config = Configuration(unit.scalaProject)
          val vmArgs = config.vmArgs
          vmRunnerConfig.setVMArguments(vmArgs.args)

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
          debugEventListener = new DebugEventListener(self, launchRef, terminalMessage)
          DebugPlugin.getDefault().addDebugEventListener(debugEventListener)

          // launch the vm
          vmRunner.run(vmRunnerConfig, launch, new NullProgressMonitor)

          // We have a process at this point
          val process = getFirstProcess(launch).get

          // connect the out and error streams to the buffer used by the mixer
          connectListener(process.getStreamsProxy().getOutputStreamMonitor(), stdoutWriter)

          val maxOutput = WorksheetPlugin.plugin.getPreferenceStore().getInt(WorksheetPreferences.P_CUTOFF_VALUE)
          // start the mixer
          editorUpdater = context.actorOf(IncrementalDocumentMixer.props(doc, stdoutWriter, maxOutput), s"incremental-document-mixer-for-unit-${unit.file.name}")

          editorUpdater ! IncrementalDocumentMixer.StartUpdatingDocument
          // switch behavior. Waits for the end or the interruption of the evaluation
          context.become(evaluatingProgram, discardOld = false)
  }

  private def evaluatingProgram: Receive = {
    case msg: StopRun if (msg.getId == currentRunId) => reset()
    case FinishedRun => reset()
  }

  private def reset(): Unit = {
    context.unbecome()

    if (editorUpdater != null) {
      editorUpdater ! IncrementalDocumentMixer.StopUpdatingDocument
      context.stop(editorUpdater)
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

    currentRunId = null
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