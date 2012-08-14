package org.scalaide.worksheet.lexical

import scala.tools.eclipse.lexical.AbstractScalaScanner
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClasses

import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token

import WorksheetSyntaxClasses.EVAL_RESULT_FIRST_LINE
import WorksheetSyntaxClasses.EVAL_RESULT_NEW_LINE

class SingleLineCommentScanner(val colorManager: IColorManager, val preferenceStore: IPreferenceStore) extends AbstractScalaScanner {
  ColorInitializer.init(preferenceStore)

  abstract sealed class State

  case object Init extends State
  case object MiddleFirst extends State
  case object MiddleNew extends State
  case object End extends State

  private var offset: Int = _
  private var length: Int = _
  private var state: State = End
  private var document: IDocument = _
  var tokenOffset: Int = -1
  var tokenLength: Int = -1

  def setRange(document: IDocument, offset: Int, length: Int) {
    this.document = document
    this.offset = offset
    this.length = length
    this.state = Init
  }

  def nextToken(): IToken =
    state match {
      case Init => {
        val c = document.getChar(offset + "//".length())
        import WorksheetSyntaxClasses._
        def commonWork(c: Char) = {
          tokenOffset = offset
          tokenLength = "//>".length()
          offset += tokenLength;
          getToken(EVAL_RESULT_MARKER)
        }
        c match {
          case '>' => {
            state = MiddleFirst
            commonWork(c)
          }
          case '|' => {
            state = MiddleNew
            commonWork(c)
          }
          case _ => {
            tokenOffset = offset
            tokenLength = length
            offset += tokenLength;
            state = End
            getToken(ScalaSyntaxClasses.SINGLE_LINE_COMMENT)
          }
        }
      }
      case End => {
        Token.EOF
      }
      case MiddleFirst => {
        tokenOffset = offset
        tokenLength = length - "//>".length()
        offset += tokenLength;
        state = End
        getToken(EVAL_RESULT_FIRST_LINE)
      }
      case MiddleNew => {
        tokenOffset = offset
        tokenLength = length - "//>".length()
        offset += tokenLength;
        state = End
        getToken(EVAL_RESULT_NEW_LINE)
      }
    }

  def getTokenOffset = tokenOffset

  def getTokenLength = tokenLength
}
