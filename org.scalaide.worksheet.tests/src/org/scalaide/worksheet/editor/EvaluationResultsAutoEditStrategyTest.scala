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

  @Test
  def addNewLineBetweenCodeAndResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code//>some result\nmore code",
      /*base offset*/ 32,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 46,
      /*expected length*/ 0,
      /*expected replacement text*/ "\n")
  }

  @Test
  def addNewLineBetweenCodeAndResultCommentInWhiteSpaces() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 35,
      /*base length*/ 0,
      /*base replacement text*/ "\n",
      /*expected offset*/ 51,
      /*expected length*/ 0,
      /*expected replacement text*/ "\n")
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
      /*expected length*/ 22,
      /*expected replacement text*/ "       //>some result\nde\n")
  }
  
  @Test
  def replaceCodeByNewLineInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 25,
      /*base length*/ 4,
      /*base replacement text*/ "\n",
      /*expected offset*/ 25,
      /*expected length*/ 27,
      /*expected replacement text*/ "            //>some result\node\n")
  }

  @Test
  def replaceCodeAndSomePaddingByNewLineInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 29,
      /*base length*/ 5,
      /*base replacement text*/ "\n",
      /*expected offset*/ 29,
      /*expected length*/ 23,
      /*expected replacement text*/ "        //>some result\n\n")
  }
  
  @Test
  def replaceCodeAndSomePaddingBySomeCodeWithNewLinesInCodeWithResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\nmore code",
      /*base offset*/ 28,
      /*base length*/ 6,
      /*base replacement text*/ "new code\nmore new code\nand more",
      /*expected offset*/ 28,
      /*expected length*/ 24,
      /*expected replacement text*/ "new code //>some result\nmore new code\nand more\n")
  }
  
  @Test
  def replaceCodeAndSomePaddingBySomeCodeWithNewLinesInCodeWithExtraLineResultComment() {
    checkCommand(
      /*base text*/ "a bit of code to start\nsome code     //>some result\n              //| more result\nmore code",
      /*base offset*/ 60,
      /*base length*/ 4,
      /*base replacement text*/ "new code\nmore code\nand more",
      /*expected offset*/ 60,
      /*expected length*/ 22,
      /*expected replacement text*/ "new code //| more result\nmore code\nand more\n")
  }
  
  private def checkCommand(baseText: String, baseOffset: Int, baseLength: Int, baseReplacementText: String, expectedOffset: Int, expectedLenght: Int, expectedReplacementText: String) {
    val document = new TestDocument(baseText)
    val command = TestCommand(baseOffset, baseLength, baseReplacementText)

    new EvaluationResultsAutoEditStrategy().customizeDocumentCommand(document, command)

    assertEquals("Wrong offset", expectedOffset, command.offset)
    assertEquals("Wrong length", expectedLenght, command.length)
    assertEquals("Wrong replacement text", expectedReplacementText, command.text)
  }

}

object TestCommand {
  def apply(offset: Int, length: Int, replacementText: String): TestCommand = {
    val command = new TestCommand()
    command.offset = offset
    command.length = length
    command.text = replacementText
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
      val mid= start + ((start + end) / 2)
      val line= lines(mid)
      if (offset < line._1) {
        binarySearch(start, mid - 1)
      } else if (offset >= line._3) {
        binarySearch(mid + 1, end)
      } else {
        mid
      }
    }
    
    
    if (offset < 0 || text.length() < offset) {
      throw new BadLocationException
    }
    
    binarySearch(0, lines.size)
  }
  
  override def getLineInformation(lineNb: Int): IRegion = {
    val line= lines(lineNb)
    new Region(line._1, line._2)
  }
  
  override def getLineDelimiter(lineNb: Int): String = {
    val line= lines(lineNb)
    val delimiterOffset= line._1 + line._2
    get(delimiterOffset, line._3 - delimiterOffset)
  }

}