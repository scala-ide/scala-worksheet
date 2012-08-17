package org.scalaide.worksheet.properties

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass

import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.Viewer
import org.scalaide.worksheet.lexical.SyntaxClasses

class SyntaxColouringTreeContentAndLabelProvider extends LabelProvider with ITreeContentProvider {

  override def getElements(inputElement: AnyRef): Array[AnyRef] = SyntaxClasses.AllSyntaxClasses.toArray

  override def getChildren(parentElement: AnyRef) = Array()

  override def getParent(element: AnyRef) = null

  override def hasChildren(element: AnyRef) = false

  override def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

  override def getText(element: AnyRef) = element match {
    case ScalaSyntaxClass(displayName, _, _) => displayName
  }
}