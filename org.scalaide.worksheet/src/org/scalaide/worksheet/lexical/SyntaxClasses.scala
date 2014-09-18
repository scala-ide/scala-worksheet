package org.scalaide.worksheet.lexical

import org.scalaide.ui.syntax.ScalaSyntaxClass
import org.scalaide.ui.syntax.ScalaSyntaxClass.Category

object SyntaxClasses {
  val FIRST_LINE = ScalaSyntaxClass("Evaluation result - First line", "syntaxColouring.evalResultFirstLine")
  val NEW_LINE = ScalaSyntaxClass("Evaluation result - New line", "syntaxColouring.evalResultNewLine")
  val MARKER = ScalaSyntaxClass("Evaluation result - Marker", "syntaxColouring.evalResultMarker")
  val DELIMITER = ScalaSyntaxClass("Evaluation result - Delimiter", "syntaxColouring.evalResultDelimiter")

  val resultCategory = Category("Result", List(FIRST_LINE, NEW_LINE, MARKER, DELIMITER))
  
  val categories = List(resultCategory)

  val ALL_SYNTAX_CLASSES = categories.flatMap(_.children)
}