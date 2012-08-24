package org.scalaide.worksheet.editor

import org.junit.Test
import org.junit.Assert._
import org.eclipse.jdt.internal.core.util.SimpleDocument
import org.eclipse.jface.text.DocumentCommand
import org.eclipse.jface.text.TextUtilities
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import scala.annotation.tailrec
import org.eclipse.jface.text.BadLocationException

class EvaluationResultsAutoEditStrategyTest {

  // TODO: same test, with one space padding
  @Test
  def addNewLineBetweenCodeAndResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code//>some result\nmore code",
      /*base offset*/ 32,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 32,
      /*expected length*/ 14,
      /*expected replacement text*/ " //>some result\n",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 48)
  }

  @Test
  def addNewLineBetweenCodeAndResultCommentInWhiteSpaces() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 35,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 35,
      /*expected length*/ 16,
      /*expected replacement text*/ "  //>some result\n",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 52)
  }

  @Test
  def addNewLineInResultComment() {
    // no change
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 38,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 38,
      /*expected length*/ 0,
      /*expected replacement text*/ "\n")
  }

  @Test
  def addNewLineInCodeWithoutResultComment() {
    // no change
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code\nmore code",
      /*base offset*/ 30,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 30,
      /*expected length*/ 0,
      /*expected replacement text*/ "\n")
  }

  @Test
  def addSomeCodeWithNewLineOverTheResultComment() {
    // no change
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 30,
      /*base length*/ 10,
      /*base replacement text*/ "new code\nmore new code",
      /*expected offset*/ 30,
      /*expected length*/ 10,
      /*expected replacement text*/ "new code\nmore new code")
  }

  @Test
  def addNewLineInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 30,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 30,
      /*expected length*/ 21,
      /*expected replacement text*/ "       //>some result\nde",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 52)
  }

  @Test
  def replaceCodeByNewLineInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 25,
      /*base length*/ 4,
      /*base replacement text*/ "\n",
      /*expected offset*/ 25,
      /*expected length*/ 26,
      /*expected replacement text*/ "            //>some result\node",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 52)
  }

  @Test
  def replaceCodeAndSomePaddingByNewLineInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 29,
      /*base length*/ 5,
      /*base replacement text*/ "\n",
      /*expected offset*/ 29,
      /*expected length*/ 22,
      /*expected replacement text*/ "        //>some result\n",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 52)
  }

  @Test
  def replaceCodeAndSomePaddingBySomeCodeWithNewLinesInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 28,
      /*base length*/ 6,
      /*base replacement text*/ "new code\nmore new code\nand more",
      /*expected offset*/ 28,
      /*expected length*/ 23,
      /*expected replacement text*/ "new code //>some result\nmore new code\nand more",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 74)
  }

  @Test
  def replaceCodeAndSomePaddingBySomeCodeWithNewLinesInCodeWithExtraLineResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\n              //| more result\nmore code",
      /*base offset*/ 60,
      /*base length*/ 4,
      /*base replacement text*/ "new code\nmore code\nand more",
      /*expected offset*/ 60,
      /*expected length*/ 21,
      /*expected replacement text*/ "new code //| more result\nmore code\nand more",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 103)
  }

  @Test
  def deleteLineAfterResultComment() {
    checkCommand(
      /*base text*/ "a bit of code\nsome code     //>some result\nmore code",
      /*base offset*/ 42,
      /*base length*/ 1,
      /*base replacement text*/ "",
      /*expected offset*/ 23,
      /*expected length*/ 29,
      /*expected replacement text*/ "more code //>some result",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 23)
  }

  @Test
  def deleteLineAndCodeAfterResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 51,
      /*base length*/ 8,
      /*base replacement text*/ "",
      /*expected offset*/ 32,
      /*expected length*/ 29,
      /*expected replacement text*/ "de   //>some result",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 32)
  }

  @Test
  def deleteLineAndKeepNextLineComment() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code   //>more result\neven more code",
      /*base offset*/ 42,
      /*base length*/ 1,
      /*base replacement text*/ "",
      /*expected offset*/ 23,
      /*expected length*/ 46,
      /*expected replacement text*/ "more code //>more result",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 23)
  }

  @Test
  def deleteMultiLineOverResultComment() {
    // no change
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 40,
      /*base length*/ 5,
      /*base replacement text*/ "",
      /*expected offset*/ 40,
      /*expected length*/ 5,
      /*expected replacement text*/ "")
  }

  @Test
  def addSomeCodeInCode() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 20,
      /*base length*/ 0,
      /*base replacement text*/ "abcd",
      /*expected offset*/ 20,
      /*expected length*/ 7,
      /*expected replacement text*/ "abcdode",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 24)
  }

  @Test
  def addSomeCodeInCodeNotEnoughPadding() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 22,
      /*base length*/ 0,
      /*base replacement text*/ "abcdef",
      /*expected offset*/ 22,
      /*expected length*/ 5,
      /*expected replacement text*/ "abcdefe",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 28)
  }

  @Test
  def removeSomeCodeInCode() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 18,
      /*base length*/ 3,
      /*base replacement text*/ "",
      /*expected offset*/ 18,
      /*expected length*/ 5,
      /*expected replacement text*/ "de   ",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 18)
  }

  @Test
  def addSomeCodeInPadding() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 25,
      /*base length*/ 0,
      /*base replacement text*/ "aa",
      /*expected offset*/ 25,
      /*expected length*/ 2,
      /*expected replacement text*/ "aa")
  }

  @Test
  def addSomeCodeInPaddingWithResultShift() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 24,
      /*base length*/ 0,
      /*base replacement text*/ "bbbb",
      /*expected offset*/ 24,
      /*expected length*/ 3,
      /*expected replacement text*/ "bbbb")
  }

  @Test
  def addSpacesInPadding() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 24,
      /*base length*/ 0,
      /*base replacement text*/ "  ",
      /*expected offset*/ 24,
      /*expected length*/ 2,
      /*expected replacement text*/ "  ")
  }

  @Test
  def replaceMultilineWithCodeByMultiLine() {
    checkCommand(
      /*base text*/ "code to start\naaatext to replace     //>some result\nmore text to replacesome code     //>some more result\nfinal code",
      /*base offset*/ 17,
      /*base length*/ 55,
      /*base replacement text*/ "the new code\nis here",
      /*expected offset*/ 17,
      /*expected length*/ 88,
      /*expected replacement text*/ "the new code                   //>some more result\nis heresome code",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 75)
  }

  @Test
  def replaceMultilineWithCodeBySingleLine() {
    checkCommand(
      /*base text*/ "code to start\nbbbbtext to replace     //>some result\nmore text to replacesome code     //>some more result\nfinal code",
      /*base offset*/ 18,
      /*base length*/ 55,
      /*base replacement text*/ "the new code is here",
      /*expected offset*/ 18,
      /*expected length*/ 88,
      /*expected replacement text*/ "the new code is heresome code //>some more result",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 38)
  }

  @Test
  def replaceNewLineByOneLine() {
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code\nfinal code",
      /*base offset*/ 42,
      /*base length*/ 1,
      /*base replacement text*/ "abc",
      /*expected offset*/ 23,
      /*expected length*/ 29,
      /*expected replacement text*/ "abcmore code //>some result",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 26)
  }

  @Test
  def replaceNewLineByMultiLine() {
    checkCommand(
      /*base text*/ "code to start\nsome additional code     //>some result\nmore code\nfinal code",
      /*base offset*/ 53,
      /*base length*/ 1,
      /*base replacement text*/ "abc\ndefg",
      /*expected offset*/ 34,
      /*expected length*/ 29,
      /*expected replacement text*/ "abc  //>some result\ndefgmore code",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 58)
  }

  @Test
  def removeLastCharacter() {
    // no change
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 51,
      /*base length*/ 1,
      /*base replacement text*/ "",
      /*expected offset*/ 51,
      /*expected length*/ 1,
      /*expected replacement text*/ "")
  }

  @Test
  def removeLastCharacterInResultComment() {
    // no change
    checkCommand(
      /*base text*/ "code to start\nsome code     //>some result\nmore code   //> more result",
      /*base offset*/ 69,
      /*base length*/ 1,
      /*base replacement text*/ "",
      /*expected offset*/ 69,
      /*expected length*/ 1,
      /*expected replacement text*/ "")
  }

  @Test
  def removeLastCharacterIsANewLineWithResultCommentPreviousLine() {
    checkCommand(
      /*base text*/ "code to start\nsome short code     //>some result\n",
      /*base offset*/ 48,
      /*base length*/ 1,
      /*base replacement text*/ "",
      /*expected offset*/ 29,
      /*expected length*/ 20,
      /*expected replacement text*/ "     //>some result",
      /*expected shiftsCaret*/ false,
      /*expected caret offset*/ 29)
  }

  private def checkCommand(baseText: String, baseOffset: Int, baseLength: Int, baseReplacementText: String, expectedOffset: Int, expectedLenght: Int, expectedReplacementText: String, expectedShiftsCaret: Boolean = true, expectedCaretOffset: Int = -1) {
    val document = new TestDocument(baseText)
    val command = TestCommand(baseOffset, baseLength, baseReplacementText)

    new EvaluationResultsAutoEditStrategy().customizeDocumentCommand(document, command)

    assertEquals("Wrong offset", expectedOffset, command.offset)
    assertEquals("Wrong length", expectedLenght, command.length)
    assertEquals("Wrong replacement text", expectedReplacementText, command.text)
    assertEquals("Wrong shiftsCaret state", expectedShiftsCaret, command.shiftsCaret)
    assertEquals("Wrong new caret offset", expectedCaretOffset, command.caretOffset)
  }

}

object TestCommand {
  def apply(offset: Int, length: Int, replacementText: String): TestCommand = {
    val command = new TestCommand()
    command.offset = offset
    command.length = length
    command.text = replacementText
    command.shiftsCaret = true
    command.caretOffset = -1
    command
  }
}

/**
 * Subclass to be able to create new DocumentCommand
 */
class TestCommand extends DocumentCommand {
}

object TestDocument {

  /**
   * generate list of tuple containing information about each line
   */
  @tailrec
  private def getLines(text: String, offset: Int, acc: Vector[(Int, Int, Int)]): Vector[(Int, Int, Int)] = {
    TextUtilities.indexOf(TextUtilities.DELIMITERS, text, offset) match {
      case Array(-1, _) =>
        acc :+ (offset, text.length - offset, text.length)
      case Array(o, i) =>
        val step = TextUtilities.DELIMITERS(i).length() + o
        getLines(text, step, acc :+ (offset, o - offset, step))
    }
  }

}

/**
 * A String based document which support operations needed by EvaluationResultsAutoEditStrategy
 */
class TestDocument(text: String) extends SimpleDocument(text) {

  // information about each line location
  private val lines: Vector[( /*offset*/ Int, /*length*/ Int, /*last Delimiter char offset*/ Int)] = TestDocument.getLines(text, 0, Vector.empty)

  override def getLegalLineDelimiters = TextUtilities.DELIMITERS

  override def getLineOfOffset(offset: Int): Int = {

    // uses a binary search to find the right line

    def binarySearch(start: Int, end: Int): Int = {
      val mid = start + ((end - start) / 2)
      val line = lines(mid)
      if (offset < line._1) {
        binarySearch(start, mid - 1)
      } else if (offset >= line._3) {
        binarySearch(mid + 1, end)
      } else {
        mid
      }
    }

    if (offset < 0 || text.length < offset) {
      throw new BadLocationException
    }

    if (offset == text.length)
      lines.size - 1
    else
      binarySearch(0, lines.size)
  }

  override def getLineInformation(lineNb: Int): IRegion = {
    val line = lines(lineNb)
    new Region(line._1, line._2)
  }

  override def getLineDelimiter(lineNb: Int): String = {
    val line = lines(lineNb)
    val delimiterOffset = line._1 + line._2
    get(delimiterOffset, line._3 - delimiterOffset)
  }

}