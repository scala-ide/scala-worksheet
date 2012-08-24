package org.scalaide.worksheet.editor

import scala.collection.mutable.StringBuilder

import org.eclipse.jface.text.DocumentCommand

import org.eclipse.jface.text.IAutoEditStrategy
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextUtilities
import org.eclipse.jface.text.IRegion
import scala.collection.mutable.StringBuilder
import scala.annotation.tailrec

object EvaluationResultsAutoEditStrategy {
}

/**
 * Smart strategy to allow users to type code like if the evaluation result comments were not there.
 */
class EvaluationResultsAutoEditStrategy extends IAutoEditStrategy {

  override def customizeDocumentCommand(document: IDocument, command: DocumentCommand) {
    DocumentCommandUpdater(document, command).checkAndUpdate
  }
}

private[editor] object DocumentCommandUpdater {

  /**
   * The markers for evaluation result comments
   */
  final val ResultDelimiters = Array("//>", "//|")

  /**
   * Initialize an instance of DocumentCommandUpdater, with the basic information needed to
   * find out if the command needs to be updated.
   */
  def apply(document: IDocument, command: DocumentCommand): DocumentCommandUpdater = {
    // the line delimiters used in the document
    val lineDelimiters: Array[String] = document.getLegalLineDelimiters

    // the offset of the the first character after the removed text
    val removedTextEnd: Int = command.offset + command.length

    // information about the line containing the start of the removed text (first line)
    val firstLineNb: Int = document.getLineOfOffset(command.offset)
    val firstLineInfo = document.getLineInformation(firstLineNb)
    val firstLineText = document.get(firstLineInfo.getOffset, firstLineInfo.getLength)

    // offset of the first character of the removed text, in the first line
    val firstLineRemovedTextStart = command.offset - firstLineInfo.getOffset

    // offset of the first character of the evaluation result comment, in the first line, if it exists
    val firstLineResultCommentOffset = firstResultDelimiter(firstLineText)

    // information about the line containing the end of the removed text (last line)
    val lastLineNb: Int = document.getLineOfOffset(removedTextEnd)

    if (firstLineNb == lastLineNb) {
      // no need to recompute most of the 'last line' values, if the removed text is on a single line

      // offset of the first character after the removed text, in the line
      val firstLineRemovedTextEnd = removedTextEnd - firstLineInfo.getOffset

      new DocumentCommandUpdater(
        document,
        command,
        firstLineNb,
        firstLineInfo,
        firstLineText,
        firstLineRemovedTextStart,
        firstLineResultCommentOffset,
        firstLineNb,
        firstLineInfo,
        firstLineText,
        firstLineRemovedTextEnd,
        firstLineResultCommentOffset,
        document.getLegalLineDelimiters)
    } else {
      // more information about the line containing the end of the removed text (last line)
      val lastLineInfo = document.getLineInformation(lastLineNb)
      val lastLineText = document.get(lastLineInfo.getOffset, lastLineInfo.getLength)

      // offset of the first character after the removed text, in the last line
      val lastLineRemovedTextEnd = removedTextEnd - lastLineInfo.getOffset

      // offset of the first character of the evaluation result comment, in the last line, if it exists
      val lastLineResultCommentOffset = firstResultDelimiter(lastLineText)

      new DocumentCommandUpdater(
        document,
        command,
        firstLineNb,
        firstLineInfo,
        firstLineText,
        firstLineRemovedTextStart,
        firstLineResultCommentOffset,
        lastLineNb,
        lastLineInfo,
        lastLineText,
        lastLineRemovedTextEnd,
        lastLineResultCommentOffset,
        document.getLegalLineDelimiters)
    }
  }

  /**
   * Return the position of the first result delimiter
   */
  private def firstResultDelimiter(text: String): Option[Int] = {
    TextUtilities.indexOf(ResultDelimiters, text, 0) match {
      case Array(-1, _) =>
        None
      case Array(offset, _) =>
        Some(offset)
    }
  }

  /**
   * Return the offset of the first line delimiter in the given text
   */
  private def firstNewLineDelimiterOffset(text: String, lineDelimiters: Array[String]): Option[Int] = {
    TextUtilities.indexOf(lineDelimiters, text, /*start offset*/ 0) match {
      case Array(-1, _) =>
        None
      case Array(offset, _) =>
        Some(offset)
    }
  }

  /**
   * Return the start offset of the padding in the given line.
   * If the offset of evaluation comment is None, return the line length (there is no padding).
   */
  private def paddingStart(line: String, evaluationCommentOffset: Option[Int]): Int = {
    evaluationCommentOffset match {
      case Some(offset) =>
        line.lastIndexWhere(!_.isWhitespace, offset - 1) + 1
      case None =>
        line.length()
    }
  }

}

private[editor] class DocumentCommandUpdater(
  /** the document the command will be applied on */
  document: IDocument,
  /** the command to be applied */
  command: DocumentCommand,
  /** line number of the line containing the start of the removed text (first line)*/
  firstLineNb: Int,
  /** info of the first line */
  firstLineInfo: IRegion,
  /** text of the first line*/
  firstLineText: String,
  /** offset of the first character of the removed text, in the first line */
  firstLineRemovedTextStart: Int,
  /** offset of the first character of the evaluation result comment, in the first line, if it exists */
  firstLineResultCommentOffset: Option[Int],
  /** line number of the line containing the end of the removed text (last line) */
  lastLineNb: Int,
  /** info of the last line */
  lastLineInfo: IRegion,
  /** text of the last line */
  lastLineText: String,
  /** offset of the first character after the removed text, in the last line */
  lastLineRemovedTextEnd: Int,
  /** offset of the first character of the evaluation result comment, in the last line, if it exists */
  lastLineResultCommentOffset: Option[Int],
  /** the line delimiters used in the document */
  lineDelimiters: Array[String]) {
  import DocumentCommandUpdater._

  /**
   * Compute the length of padding and evaluation result comment in the first line.
   * Return 0 if the first line doesn't contain an evaluation result comment.
   */
  private def paddingAndCommentLength =
    firstLineInfo.getLength - paddingStart(firstLineText, firstLineResultCommentOffset)

  /**
   * Return the text which will be shifted by this command. It is the part between the last
   * character to be removed and the start of the padding in the last line.
   * 
   * code code code code       //>result
   *  to there -->|
   *               ^^^^^ text to shift
   * 
   * if the text to remove ends in the padding section, an empty string is returned.
   */
  private lazy val codeToShift = {
    val lastLinePaddingStart = paddingStart(lastLineText, lastLineResultCommentOffset)

    if (lastLineRemovedTextEnd >= lastLinePaddingStart) {
      ""
    } else {
      lastLineText.substring(lastLineRemovedTextEnd, lastLinePaddingStart)
    }
  }

  /**
   * Check if the command needs to be modified, and then update it.
   */
  def checkAndUpdate() {
    /* updating the command is needed only if the 'last line' contains an evaluation
     * result comment, or in one case if the 'first line' contains such comment
     * 
     * the command is not updated if the first or the last character of the text
     * to be removed is situated inside an evaluation result comment, including the marker
     */
    
    /* first case: removing a new line delimiter, plus more code
     */
    if (firstLineRemovedTextStart == firstLineInfo.getLength && command.length > 0) {
      lastLineResultCommentOffset match {
        case Some(offset) if offset >= lastLineRemovedTextEnd =>
          /* With the last line containing an evaluation result comment
           * (the first line may or may not contain an evaluation result comment)
           * 
           * code code code code    //> result
           *                                  |<-- from here
           * some line
           * some line
           * code code code code    //> result
           *  to there -->|                   
           */
          updateCommand(false, -paddingAndCommentLength)
        case None =>
          firstLineResultCommentOffset match {
            case Some(offset) =>
              /* With only the first line containing an evaluation result comment
               * 
               * code code code code    //> result
               *                                  |<-- from here
               * some line
               * some line
               * code code code code
               *  to there -->|                   
               */
              updateCommand(true, -paddingAndCommentLength)
            case None =>
          }
        case _ =>
      }
    } else {
      /* second case: everything else
       * 
       * code code code code    //> result
       *       |<-- from here
       * some line
       * some line
       * code code code code    //> result
       *  to there -->|                   
       */
      lastLineResultCommentOffset match {
        // check that the 'last line' contains a comment, and the command doesn't 'break' it
        case Some(lastLineOffset) if lastLineOffset >= lastLineRemovedTextEnd =>
          firstLineResultCommentOffset match {
            // check if the 'last line' contains a comment, and if it exists, that the command doesn't 'break' it
            case Some(firstLineOffset) if firstLineOffset >= firstLineRemovedTextStart =>
              updateCommand(false, 0)
            case None =>
              updateCommand(false, 0)
            case _ =>

          }
        case _ =>
      }
    }
  }

  /**
   * Perform the update. First stage.
   * 
   * @param keepFirstResultComment indicate which result comment to keep. The comment needs to exist.
   * @param the shift needed to command.offset to correctly update the command
   */
  private def updateCommand(keepFirstResultComment: Boolean, commandOffsetDiff: Int) {
    // extract the right comment to keep, and call the next step
    if (keepFirstResultComment) {
      updateCommand(firstLineText.substring(firstLineResultCommentOffset.get), firstLineResultCommentOffset.get, commandOffsetDiff)
    } else {
      updateCommand(lastLineText.substring(lastLineResultCommentOffset.get), lastLineResultCommentOffset.get, commandOffsetDiff)
    }
  }

  /**
   * Perform the update. Second stage.
   * 
   * @param the result comment to keep.
   * @param the result comment original offset
   * @param the shift needed to command.offset to correctly update the command
   */
  private def updateCommand(resultComment: String, resultCommentOffset: Int, commandOffsetDiff: Int) {
    val newLineDelimiterOffset = firstNewLineDelimiterOffset(command.text, lineDelimiters)

    // check if the code to insert contains a new line
    newLineDelimiterOffset match {
      case Some(offset) =>
        updateCommandMultiLine(resultComment, resultCommentOffset, offset, commandOffsetDiff)
      case None =>
        if (firstLineNb == lastLineNb) {
          // special case if neither the code to remove nor the code to insert contain a new line
          updateCommandSimpleChange()
        } else {
          updateCommandSingleLine(resultComment, resultCommentOffset, commandOffsetDiff)
        }
    }
  }

  /**
   * Perform the update. Third stage, case 1: the code to insert contains a new line
   * 
   * Generate the following new code to replace the code to remove + the remainder of the last line without the line delimiter:
   * 
   * [first line code to insert] [padding] [//> result comment] [
   * remainder of
   * the code to insert] [code to shift]
   * 
   */
  private def updateCommandMultiLine(resultComment: String, resultCommentOffset: Int, newLineDelimiterOffset: Int, commandOffsetDiff: Int) {
    // length of the new padding (at least 1)
    val paddingLength = math.max(1, resultCommentOffset - firstLineRemovedTextStart - commandOffsetDiff - newLineDelimiterOffset)

    // the new code
    val newText = new StringBuilder(command.text.substring(0, newLineDelimiterOffset))
    0 until paddingLength foreach {
      i => newText.append(' ')
    }
    newText.append(resultComment)
    newText.append(command.text.substring(newLineDelimiterOffset))
    newText.append(codeToShift)

    // update the command
    command.offset = command.offset + commandOffsetDiff
    command.length = lastLineInfo.getOffset + lastLineInfo.getLength - command.offset
    // set the caret at the right location
    command.shiftsCaret = false
    command.caretOffset = command.offset + command.text.length + paddingLength + resultComment.length

    command.text = newText.toString
  }

  /**
   * Perform the update. Third stage, case 2: neither the code to remove nor the code to insert contain a new line
   * 
   * code code code code         //> result comment
   *       |<--->|
   *              ^^^^^^ padding
   * 
   * Generate the following new code to replace the code to remove + the code to shift + some of the padding:
   * 
   * [code to insert] [code to shift] [additional padding]
   */
  private def updateCommandSimpleChange() {
    // the change of padding length required
    val paddingDiff = lastLineRemovedTextEnd - firstLineRemovedTextStart - command.text.length

    // if the code to insert and the code to remove have the same length, no need to update the command
    if (paddingDiff != 0) {

      // compute the length of padding to remove, such as the resulting padding is at least 1 character long
      // (only needed if some padding has to be removed
      val paddingToRemove =
        if (paddingDiff < 0) {
          math.max(math.min(lastLineResultCommentOffset.get - lastLineRemovedTextEnd - codeToShift.length - 1, -paddingDiff), 0)
        } else {
          0
        }

      // the new code
      val newText = new StringBuilder(command.text)
      newText.append(codeToShift)
      if (paddingDiff > 0) {
        0 until paddingDiff foreach {
          i => newText.append(' ')
        }
      }

      if (paddingDiff > 0 || !codeToShift.isEmpty()) {
        // set the caret at the right location, only needed if some padding is added, or some code has to be shift
        command.shiftsCaret = false
        command.caretOffset = command.offset + command.text.length
      }
      // update the command
      command.text = newText.toString
      command.length = command.length + codeToShift.length + paddingToRemove
    }
  }
  
  /**
   * Perform the update. Third stage, case 3: the code to insert contains no new line
   * 
   * Generate the following new code to replace the code to remove + the remainder of the last line without the line delimiter:
   * 
   * [code to insert] [code to shift] [padding] [//> result comment]
   */
  private def updateCommandSingleLine(resultComment: String, resultCommentOffset: Int, commandOffsetDiff: Int) {
    // length of the new padding (at least 1)
    val paddingLength = math.max(1, resultCommentOffset - firstLineRemovedTextStart - commandOffsetDiff - command.text.length - codeToShift.length)

    // the new code
    val newText = new StringBuilder(command.text)
    newText.append(codeToShift)
    0 until paddingLength foreach {
      i => newText.append(' ')
    }
    newText.append(resultComment)

    // set the caret at the right location
    command.shiftsCaret = false
    command.offset = command.offset + commandOffsetDiff
    // update the command
    command.caretOffset = command.offset + command.text.length
    command.length = lastLineInfo.getOffset + lastLineInfo.getLength - command.offset
    command.text = newText.toString
  }

}