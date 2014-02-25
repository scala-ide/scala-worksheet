package org.scalaide.worksheet.lexical

import org.scalaide.ui.syntax.ScalaSyntaxClass

object SyntaxClasses {
  object EvalResult {
    val FirstLine = ScalaSyntaxClass("Evaluation result - First line", "syntaxColouring.evalResultFirstLine")
    val NewLine = ScalaSyntaxClass("Evaluation result - New line", "syntaxColouring.evalResultNewLine")
    val Marker = ScalaSyntaxClass("Evaluation result - Marker", "syntaxColouring.evalResultMarker")
    val Delimiter = ScalaSyntaxClass("Evaluation result - Delimiter", "syntaxColouring.evalResultDelimiter")
  }
  val AllSyntaxClasses = Array(EvalResult.FirstLine, EvalResult.NewLine, EvalResult.Marker, EvalResult.Delimiter)
}