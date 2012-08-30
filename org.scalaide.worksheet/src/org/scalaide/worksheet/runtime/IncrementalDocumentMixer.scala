package org.scalaide.worksheet.runtime

import java.io.Writer

import scala.actors.{ Actor, DaemonActor, TIMEOUT }
import scala.tools.eclipse.logging.HasLogger

import org.scalaide.worksheet.editor.EditorProxy
import org.scalaide.worksheet.text.{ Mixer, SourceInserter }

object IncrementalDocumentMixer {
  def apply(editor: EditorProxy, source: Writer, maxOutput: Int = MaximumOutputChars): Actor = {
    val incrementalMixer = new IncrementalDocumentMixer(editor, source, maxOutput)
    incrementalMixer.start()
    incrementalMixer
  }

  final val RefreshDocumentTimeout = 200
  final val MaximumOutputChars = 100
}

private class IncrementalDocumentMixer private (editor: EditorProxy, source: Writer, val maximumOutputSize: Int = IncrementalDocumentMixer.MaximumOutputChars) extends DaemonActor with HasLogger {
  import IncrementalDocumentMixer.RefreshDocumentTimeout

  private def originalCursorPosition = editor.caretOffset
  private val originalContent = editor.getContent
  private val stripped = SourceInserter.stripRight(editor.getContent.toCharArray)
  private val mixer = new Mixer

  private def currentContent: String = source.toString()

  override def act(): Unit = loop {
    reactWithin(RefreshDocumentTimeout) {
      case TIMEOUT => updateDocument(currentContent)
      case 'stop =>
        updateDocument(currentContent)
        editor.completedExternalEditorUpdate()
        exit()
      case any => exit(this.toString + ": Unsupported message " + any)
    }
  }

  private def updateDocument(newText: String): Unit = {
    if (newText.length > 0) {
      lastText = showLongRunning(pruneOutput(newText))
      val (mixed, lastInsertion) = mixer.mix(stripped, lastText.toCharArray(), originalCursorPosition)
      editor.replaceWith(mixed.mkString, lastInsertion)
    }
  }

  private var lastText = ""
  private var ticks = 0
  private val spinner = """|/-\"""

  def showLongRunning(newText: String): String = if (lastText == newText) {
    ticks += 1
    if (ticks % 4 == 0) ticks = 0
    if (newText.last == '\n') newText.init + spinner(ticks) + '\n' else newText + spinner(ticks)
  } else newText

  def pruneOutput(newText: String): String = {
    if (newText.length() > maximumOutputSize) {
      val lastNL = newText.indexOf('\n', maximumOutputSize)
      val endPos = if (lastNL == -1) newText.length else lastNL
      val displayableText = newText.substring(0, endPos)
      val suffix = if (displayableText.last == '\n') "" else "\n"
      val lastLine = displayableText.lines.toSeq.last
      val offset = lastLine.substring(0, lastLine.indexOf(' ', 0))
      logger.debug("Cutting off large output at position: " + offset)
      displayableText + suffix + offset + " Output exceeds cutoff limit. "
    } else newText
  }

  override def toString: String = "IncrementalDocumentMixer <actor>"
}