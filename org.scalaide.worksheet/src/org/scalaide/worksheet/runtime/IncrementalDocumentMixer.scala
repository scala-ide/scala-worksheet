package org.scalaide.worksheet.runtime

import java.io.Writer

import scala.actors.{Actor, DaemonActor, TIMEOUT}
import scala.tools.eclipse.logging.HasLogger

import org.scalaide.worksheet.editor.EditorProxy
import org.scalaide.worksheet.text.{Mixer, SourceInserter}

object IncrementalDocumentMixer {
  def apply(editor: EditorProxy, source: Writer): Actor = {
    val stealer = new IncrementalDocumentMixer(editor, source)
    stealer.start()
    stealer
  }

  private val RefreshDocumentTimeout = 200
}

private class IncrementalDocumentMixer private (editor: EditorProxy, source: Writer) extends DaemonActor with HasLogger {
  import IncrementalDocumentMixer.RefreshDocumentTimeout

  private val originalCursorPosition = editor.caretOffset
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
      val (mixed, newCursorPos) = mixer.mix(stripped, newText.toCharArray(), originalCursorPosition)
      editor.replaceWith(mixed.mkString, newCursorPos)
    }
  }

  override def toString: String = "IncrementalDocumentMixer <actor>"
}