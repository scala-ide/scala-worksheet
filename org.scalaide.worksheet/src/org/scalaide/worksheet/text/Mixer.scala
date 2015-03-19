package org.scalaide.worksheet.text

import java.io.{ FileInputStream, InputStreamReader, IOException }

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

import org.scalaide.logging.HasLogger

class Mixer extends HasLogger {

  protected val stdSeparator = "//> "
  protected val ctdSeparator = "//| "
  protected val sepColumn = 50
  protected val tabInc = 8

  type Comments = Seq[(Int, Array[Char])]

  def parseComments(comments: Array[Char]): Seq[(Int, Array[Char])] = {
    var idx = 0
    val buf = new ListBuffer[(Int, Array[Char])]
    while (idx < comments.length) {
      val nextSpace = comments indexOf (' ', idx)
      var nextNL = comments indexOf ('\n', nextSpace + 1)
      if (nextNL < 0) nextNL = comments.length
      try {
        try {
          buf += ((new String(comments.slice(idx, nextSpace)).toInt, comments.slice(nextSpace + 1, nextNL)))
        } finally idx = nextNL + 1
      } catch {
        case nfe: NumberFormatException =>
          logger.debug("NFE: " + nfe.getMessage(), nfe)
      }
    }
    buf.toSeq
  }

  /** Combine source characters with right-hand side comments.
   *  @param source The stripped program source
   *  @param comments The right hand side program comments, in the format produced by the worksheet
   *         evaluator. Every line consists of a source offset (an integer number) and the comment
   *         line, separated by a space.
   *  @param oldcaret The caret position in the stripped source, a value of -1 indicates caret is
   *         at end of source
   *  @return A pair consisting of
   *            - The combination of source and comments
   *            - The position of the last inserted comment
   */
  def mix(source: Array[Char], comments: Array[Char], oldcaret: Int = -1): (Array[Char], Int) = {
    val mixed = new ArrayBuffer[Char]
    var written = 0 // # written characters of original source
    var inserted = 0 // # inserted comment characters
    val caret = if (oldcaret == -1) source.length else oldcaret
    var newcaret = caret // new caret position
    def insert(str: Char*) {
      mixed ++= str
      inserted += str.length
    }
    def align() = {
      var idx = mixed.lastIndexOf('\n') + 1
      var col = 0
      while (idx < mixed.length) {
        col =
          if (mixed(idx) == '\t') (col / tabInc) * tabInc + tabInc
          else col + 1
        idx += 1
      }
      if (col > sepColumn) {
        insert('\n')
        col = 0
      }
      while (col < sepColumn) {
        insert(' ')
        col += 1
      }
    }
    for ((offset, cs) <- parseComments(comments)) {
      val sep =
        if (written < offset) {
          for (i <- written until offset) mixed += source(i)
          written = offset
          stdSeparator
        } else {
          insert('\n')
          ctdSeparator
        }
      align()
      insert(sep: _*)
      insert(cs: _*)
      if (written < caret) newcaret = scala.math.min(caret + inserted, caret)
    }
    val lastInsertionPoint = mixed.size
    mixed ++= source.view(written, source.length)
    (mixed.toArray, lastInsertionPoint)
  }

}

object Mixer extends Mixer {

  def contents(name: String): Array[Char] = {
    val page = new Array[Char](2 << 14)
    val buf = new ArrayBuffer[Char]
    val in = new FileInputStream(name)
    val rdr = new InputStreamReader(in)
    var nread = 0
    do {
      nread = rdr.read(page, 0, page.length)
      buf ++= (if (nread == page.length) page else page.take(nread))
    } while (nread >= 0)
    buf.toArray
  }

  def main(args: Array[String]) {
    val mixer = new Mixer
    try {
      require(args.length == 2, "required arguments: file1 file2")
      val source = contents(args(0))
      val comments = contents(args(1))
      val (mixed, _) = mixer.mix(source, comments)
      println(mixed.mkString)
    } catch {
      case ex: IOException =>
        println("error: " + ex.getMessage)
    }
  }
}