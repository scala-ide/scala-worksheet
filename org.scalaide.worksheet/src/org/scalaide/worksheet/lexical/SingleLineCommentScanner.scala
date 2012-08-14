package org.scalaide.worksheet.lexical

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClasses

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.Token
import org.eclipse.jface.util.PropertyChangeEvent

import WorksheetSyntaxClasses.EVAL_RESULT_FIRST_LINE
import WorksheetSyntaxClasses.EVAL_RESULT_MARKER
import WorksheetSyntaxClasses.EVAL_RESULT_NEW_LINE

class SingleLineCommentScanner(val scalaPreferenceStore: IPreferenceStore, val worksheetPreferenceStore: IPreferenceStore) extends ITokenScanner {
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

  private var tokens: Map[ScalaSyntaxClass, Token] = Map()

  protected def getToken(syntaxClass: ScalaSyntaxClass): Token =
    tokens.getOrElse(syntaxClass, createToken(syntaxClass))

  private def createToken(syntaxClass: ScalaSyntaxClass) = {
    val token = new Token(getTextAttribute(syntaxClass))
    tokens = tokens + (syntaxClass -> token)
    token
  }

  def adaptToPreferenceChange(event: PropertyChangeEvent) =
    for ((syntaxClass, token) <- tokens)
      token.setData(getTextAttribute(syntaxClass))

  private def getTextAttribute(syntaxClass: ScalaSyntaxClass) = {
    val prefStore = syntaxClass match {
      case EVAL_RESULT_FIRST_LINE | EVAL_RESULT_NEW_LINE | EVAL_RESULT_MARKER => worksheetPreferenceStore
      case _ => scalaPreferenceStore
    }
    syntaxClass.getTextAttribute(prefStore)
  }

  def getTokenOffset = tokenOffset

  def getTokenLength = tokenLength
}
