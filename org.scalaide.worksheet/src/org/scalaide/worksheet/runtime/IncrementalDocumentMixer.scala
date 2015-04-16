package org.scalaide.worksheet.runtime

import java.io.Writer

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

import org.scalaide.logging.HasLogger
import org.scalaide.worksheet.editor.DocumentHolder
import org.scalaide.worksheet.text.{ Mixer, SourceInserter }

import akka.actor.Actor
import akka.actor.Props

object IncrementalDocumentMixer {
  def props(editor: DocumentHolder, source: Writer, maxOutput: Int): Props =
    Props(new IncrementalDocumentMixer(editor, source, maxOutput))

  private final val RefreshDocumentTimeout: FiniteDuration = 200 millis

  /** How many ticks should pass with output not changing before adding a spinner? */
  private final val InfiniteLoopTicks: Int = (1000 / RefreshDocumentTimeout.length).toInt

  private case object UpdateDocument
  case object StartUpdatingDocument
  case object StopUpdatingDocument
}

private class IncrementalDocumentMixer private (editor: DocumentHolder, source: Writer, val maximumOutputSize: Int) extends Actor with HasLogger {
  import IncrementalDocumentMixer.{ RefreshDocumentTimeout, InfiniteLoopTicks, UpdateDocument, StartUpdatingDocument, StopUpdatingDocument }

  private val stripped = SourceInserter.stripRight(editor.getContents.toCharArray)
  private val mixer = new Mixer

  private def currentContent: String = source.toString()

  override def receive = {
    case StartUpdatingDocument =>
      scheduleDocumentUpdate()
    case UpdateDocument =>
      updateDocument(currentContent)
      scheduleDocumentUpdate()
    case StopUpdatingDocument =>
      updateDocument(currentContent)
      editor.endUpdate()
      context.stop(self)
  }

  private def scheduleDocumentUpdate(): Unit = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(RefreshDocumentTimeout, self, UpdateDocument)
  }

  private def updateDocument(newText: String): Unit = {
    if (newText.length > 0) {
      val (mixed, lastInsertion) = mixer.mix(stripped, showLongRunning(pruneOutputPerEvaluation(newText)).toCharArray())
      editor.replaceWith(mixed.mkString, lastInsertion)
    }
  }

  private var lastText = ""
  private var ticks = -InfiniteLoopTicks
  private val spinner = """|/-\"""

  def showLongRunning(newText: String): String = {
    val prevText = lastText
    lastText = newText

    if (prevText == newText) {
      def addSpinner: String = {
        ticks %= 4
        if (newText.last == '\n') newText.init + spinner(ticks) + '\n' else newText + spinner(ticks)
      }

      ticks += 1
      if (ticks >= 0) addSpinner else newText
    }
    else {
      // reset the counter and wait another second before adding a spinner
      ticks = -InfiniteLoopTicks
      newText
    }
  }

  def pruneOutputPerEvaluation(newText: String): String = {
    // Groups together `lines` that share the same offset (lines order is maintained)
    def groupLinesByOffset(lines: List[String]): LinkedHashMap[String, ListBuffer[String]] = {
      val offsetExtractor = """^(\d*)""".r
      val groupedEvaluations = LinkedHashMap.empty[String, ListBuffer[String]]

      for (line <- lines) offsetExtractor.findFirstIn(line) match {
        case Some(offset) =>
          groupedEvaluations.get(offset) match {
            case None         => groupedEvaluations += (offset -> ListBuffer(line))
            case Some(result) => groupedEvaluations += ((offset, result += line))
          }
        case None =>
          logger.error("Discarded evaluation result, this is a bug. Please open a ticket and make sure to provide the worksheet source that generated this warning.")
      }

      groupedEvaluations
    }

    val lines = newText.split('\n')
    val linesByOffset = groupLinesByOffset(lines.toList)

    val evaluationResults = for ((offset, evaluationOutputs) <- linesByOffset) yield {
      val evaluationResult = evaluationOutputs.mkString("\n")
      (if (evaluationResult.length <= maximumOutputSize) evaluationResult
      else {
        logger.debug("Cutting off large output at position: " + offset)
        evaluationResult.take(maximumOutputSize) + '\n' + offset + " Output exceeds cutoff limit."
      })
    }

    evaluationResults.mkString("\n")
  }

  override def toString: String = "IncrementalDocumentMixer <actor>"
}