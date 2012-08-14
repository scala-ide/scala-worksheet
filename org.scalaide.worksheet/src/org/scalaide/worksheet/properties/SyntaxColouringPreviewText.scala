package org.scalaide.worksheet.properties

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass

class SyntaxColouringPreviewText {

  val previewText = """some code   //> Hello
some code   //| World
"""

  case class ColouringLocation(syntaxClass: ScalaSyntaxClass, offset: Int, length: Int)

}