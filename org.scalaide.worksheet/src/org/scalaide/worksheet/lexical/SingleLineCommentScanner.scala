package org.scalaide.worksheet.lexical

import org.scalaide.ui.syntax.ScalaSyntaxClass
import org.scalaide.ui.syntax.ScalaSyntaxClasses

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.rules.Token
import org.eclipse.jface.util.PropertyChangeEvent
import org.scalaide.worksheet.lexical.SyntaxClasses.EvalResult

class SingleLineCommentScanner(val scalaPreferenceStore: IPreferenceStore, val worksheetPreferenceStore: IPreferenceStore) extends ITokenScanner {
  import SingleLineCommentScanner._
  abstract sealed class State

  case object Init extends State
  case object Delimiter extends State
  case object MiddleFirst extends State
  case object MiddleNew extends State
  case object End extends State

  private var offset: Int = _
  private var length: Int = _
  private var state: State = End
  private var document: IDocument = _
  var tokenOffset: Int = -1
  var tokenLength: Int = -1

  override def setRange(document: IDocument, offset: Int, length: Int) {
    this.document = document
    this.offset = offset
    this.length = length
    this.state = Init
  }

  override def nextToken(): IToken = {
    def commonWorkMiddle(markerDelimiterString: String) = {
      tokenOffset = offset
      tokenLength = length - markerDelimiterString.length()
      offset += tokenLength;
      this.state = End
    }

    state match {
      case Init => {
        val c = document.getChar(offset + MARKER_STRING.length())
        c match {
          case FIRST_LINE_CHAR | NEW_LINE_CHAR => {
            tokenOffset = offset
            tokenLength = MARKER_STRING.length()
            offset += tokenLength;
            state = Delimiter
            getToken(EvalResult.Marker)
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
      case Delimiter => {
        val c = document.getChar(offset)
        state = c match {
          case FIRST_LINE_CHAR => MiddleFirst
          case NEW_LINE_CHAR => MiddleNew
        }
        tokenOffset = offset
        tokenLength = 1 // Delimiter length is 1
        offset += tokenLength;
        getToken(EvalResult.Delimiter)
      }
      case MiddleFirst => {
        commonWorkMiddle(FIRST_LINE_STRING)
        getToken(EvalResult.FirstLine)
      }
      case MiddleNew => {
        commonWorkMiddle(NEW_LINE_STRING)
        getToken(EvalResult.NewLine)
      }
      case End => {
        Token.EOF
      }
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
      case ScalaSyntaxClasses.SINGLE_LINE_COMMENT => scalaPreferenceStore
      case _ => worksheetPreferenceStore
    }
    syntaxClass.getTextAttribute(prefStore)
  }

  override def getTokenOffset = tokenOffset

  override def getTokenLength = tokenLength
}

object SingleLineCommentScanner {
  private val FIRST_LINE_CHAR = '>'
  private val NEW_LINE_CHAR = '|'
  private val MARKER_STRING = "//"
  private val FIRST_LINE_STRING = MARKER_STRING + FIRST_LINE_CHAR
  private val NEW_LINE_STRING = MARKER_STRING + NEW_LINE_CHAR
}