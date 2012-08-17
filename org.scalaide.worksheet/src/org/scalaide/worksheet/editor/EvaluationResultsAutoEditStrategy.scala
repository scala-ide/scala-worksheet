package org.scalaide.worksheet.editor

import scala.collection.mutable.StringBuilder

import org.eclipse.jface.text.DocumentCommand

import org.eclipse.jface.text.IAutoEditStrategy
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextUtilities

object EvaluationResultsAutoEditStrategy {
  final val ResultDelimiters = Array("//>", "//|")
}

/**
 * Smart strategy to allow users to type code like if the evaluation result comment where not there.
 */
class EvaluationResultsAutoEditStrategy extends IAutoEditStrategy {

  override def customizeDocumentCommand(document: IDocument, command: DocumentCommand) {
    /*
     * Adjust the command only if it is adding at least one line delimiter,
     * on a line containing an evaluation result comment,
     * and which is not touching the result comment.
     */
    val lineDelimiters = document.getLegalLineDelimiters()

    // additional work is needed only if the new text contains a new line
    firstNewLineDelimiter(command.text, lineDelimiters) foreach {
      (t: (String, Int)) =>
        val lineNb = document.getLineOfOffset(command.offset)
        val line = document.getLineInformation(lineNb)

        // and if the line contains an evaluation result
        firstResultDelimiter(document.get(line.getOffset, line.getLength)) foreach {
          // which is not part of the replaced text
          resultOffset =>
            if (resultOffset + line.getOffset >= command.offset + command.length) {
              fixCommand(document, command, lineNb, resultOffset, t._1, t._2) // TODO: do I need the line delimiter?
            }
        }
    }
  }

  /**
   * Does the adjusting
   */
  private def fixCommand(document: IDocument, command: DocumentCommand, lineNb: Int, resultCommentOffset: Int, replacementTextLineDelimiter: String, replacementTextLineDelimiterOffset: Int) {
    // the current line information
    val lineInfo = document.getLineInformation(lineNb)
    // the text of the current line 
    val lineText = document.get(lineInfo.getOffset, lineInfo.getLength)

    // the start offset of the padding section between the code and the result comment
    val paddingOffset =
      if (resultCommentOffset == 0) {
        0
      } else {
        lineText.lastIndexWhere(!_.isWhitespace, resultCommentOffset - 1) + 1
      }

    // the start offset of the replaced code, in the current line
    val commandOffsetInLine = command.offset - lineInfo.getOffset

    /*
     * The replacement text is split in the following sections:
     * 
     * replacement text before the first new line delimiter | replacement text first new line delimiter | remainder of the replacement text
     * 
     */
    // the text before the first new line delimiter
    val replacementTextBeginning = command.text.substring(0, replacementTextLineDelimiterOffset)
    // everything else...
    val replacementTextEnd = command.text.substring(replacementTextLineDelimiterOffset + replacementTextLineDelimiter.length)

    if (commandOffsetInLine >= paddingOffset && replacementTextBeginning.isEmpty()) {
      // insertion between code and result comment, and the replacement text start by the line delimiter
      // just shift the command to the end of the line
      command.offset = lineInfo.getOffset + lineInfo.getLength
    } else {
      /*
       * The line on which the command is executed is split is the following sections:
       * 
       * ...text before the command | code replaced by the command | padding replaced by the command | remaining padding | result comment | line delimiter
       * 
       * or
       * 
       * ...text before the command | code replaced by the command | code to shift | remaining padding | result comment | line delimiter
       * 
       */
      
      // start offset of the section of padding replaced. Is equal to the command offset, if the command start in the middle of the padding
      val replacedPaddingStart= scala.math.max(paddingOffset, commandOffsetInLine)

      // the code replaced by the command, the length of the padding section replaced by the command, the code to shift
      val (replacedCode, replacedPaddingLength, codeToShift) =
        if (commandOffsetInLine + command.length > paddingOffset) {
          // replace some of the existing padding
          (lineText.substring(commandOffsetInLine, replacedPaddingStart), commandOffsetInLine + command.length - replacedPaddingStart, "")
        } else {
          // replace only code
          (lineText.substring(commandOffsetInLine, commandOffsetInLine + command.length), 0, lineText.substring(commandOffsetInLine + command.length, paddingOffset))
        }
      // the length of the padding section untouched
      val remainingPaddingLength = resultCommentOffset - replacedPaddingStart - replacedPaddingLength
      // the result comment
      val resultComment = lineText.substring(resultCommentOffset)
      // the current line delimiter
      val lineDelimiter = document.getLineDelimiter(lineNb)
      /*
       * Otherwise, generate the following new replacement code:
       * 
       * replacement text before the first new line delimiter | recomputed padding | result comment | replacement text first new line delimiter | remainder of the replacement text | code to shift | line delimiter
       * 
       * The length of the recomputed padding is the length of 'code replaced by the command' + length of the padding section replaced by the command + length of the code to shift - the length of 'replacement text before the first new line delimiter' + the length of the padding section untouched , with a minimum value of 1
       * 
       * And the length of the replaced text is everything after the command offset, including the new line delimiter
       */

      val recomputedPadding = scala.math.max(replacedCode.length + replacedPaddingLength + codeToShift.length - replacementTextBeginning.length + remainingPaddingLength, 1)

      val newText = new StringBuilder(replacementTextBeginning)
      1 to recomputedPadding foreach {
        i => newText.append(' ')
      }
      newText.append(resultComment)
      newText.append(replacementTextLineDelimiter)
      newText.append(replacementTextEnd)
      newText.append(codeToShift)
      newText.append(lineDelimiter)
      command.text = newText.toString

      command.length = lineInfo.getLength() - commandOffsetInLine + lineDelimiter.length
    }
  }

  private def pad(length: Int): String = {
    ""
  }

  /**
   * Return the first line delimiter, and its position
   */
  private def firstNewLineDelimiter(text: String, lineDelimiters: Array[String]): Option[(String, Int)] = {
    TextUtilities.indexOf(lineDelimiters, text, /*start offset*/ 0) match {
      case Array(-1, _) =>
        None
      case Array(offset, index) =>
        Some((lineDelimiters(index), offset))
    }
  }

  /**
   * Return the position of the first result delimiter
   */
  private def firstResultDelimiter(text: String): Option[Int] = {
    TextUtilities.indexOf(EvaluationResultsAutoEditStrategy.ResultDelimiters, text, 0) match {
      case Array(-1, _) =>
        None
      case Array(offset, _) =>
        Some(offset)
    }
  }

}