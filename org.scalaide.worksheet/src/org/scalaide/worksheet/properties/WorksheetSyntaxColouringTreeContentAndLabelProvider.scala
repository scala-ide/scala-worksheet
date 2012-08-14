package org.scalaide.worksheet.properties

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass

import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.Viewer
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses

object WorksheetSyntaxColouringTreeContentAndLabelProvider extends LabelProvider with ITreeContentProvider {

  def getElements(inputElement: AnyRef) = WorksheetSyntaxClasses.ALL_SYNTAX_CLASSES.toArray

  def getChildren(parentElement: AnyRef) = Array()

  def getParent(element: AnyRef) = null

  def hasChildren(element: AnyRef) = false

  def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

  override def getText(element: AnyRef) = element match {
    case ScalaSyntaxClass(displayName, _, _) => displayName
  }
}