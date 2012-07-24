package org.scalaide.worksheet.editors

import org.eclipse.ui._
import org.eclipse.swt.graphics.Image
import scala.tools.eclipse.ScalaSourceFileEditor
import org.eclipse.ui.editors.text.TextEditor
import org.eclipse.jface.text.source._
import org.eclipse.jface.text._
import org.eclipse.jface.viewers.ISelection
import scala.tools.eclipse.ScalaSourceViewerConfiguration
import scala.tools.eclipse.ScalaPlugin
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.swt.widgets.Composite
import scala.collection.JavaConverters._

object ScriptEditor {
  
  /** The annotation types showin when hovering on the left-side ruler (or in the status bar). */
  val annotationsShownInHover = Set(
    "org.eclipse.jdt.ui.error", "org.eclipse.jdt.ui.warning", "org.eclipse.jdt.ui.info")
}

/** A Scala script editor. 
 */
class ScriptEditor extends TextEditor with SelectionTracker {

  setPartName("Scala Script Editor")
  setDocumentProvider(new ScriptDocumentProvider)

  override def initializeEditor() {
    super.initializeEditor()
    setSourceViewerConfiguration(new ScriptConfiguration(this))
  }

  /** Return the annotation model associated with the current document. */
  private def annotationModel = getDocumentProvider.getAnnotationModel(getEditorInput).asInstanceOf[IAnnotationModel with IAnnotationModelExtension2]

  def selectionChanged(selection: ITextSelection) {
    val iterator = annotationModel.getAnnotationIterator(selection.getOffset, selection.getLength, true, true).asInstanceOf[java.util.Iterator[Annotation]]
    val msg = iterator.asScala.find(a => ScriptEditor.annotationsShownInHover(a.getType)).map(_.getText).getOrElse(null)
    setStatusLineErrorMessage(msg)
  }
}
