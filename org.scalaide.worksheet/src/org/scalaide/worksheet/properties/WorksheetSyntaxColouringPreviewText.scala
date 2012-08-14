package org.scalaide.worksheet.properties

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass

object WorksheetSyntaxColouringPreviewText {

  val previewText = """some code   //> Hello
some code   //| World
"""

  case class ColouringLocation(syntaxClass: ScalaSyntaxClass, offset: Int, length: Int)

}