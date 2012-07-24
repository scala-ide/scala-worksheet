package org.scalaide.worksheet.editors

import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.text.source.IAnnotationModelExtension2
import org.eclipse.ui.editors.text.TextEditor

import scala.collection.JavaConverters.asScalaIteratorConverter

object ScriptEditor {
  
  /** The annotation types showin when hovering on the left-side ruler (or in the status bar). */
  val annotationsShownInHover = Set(
    "org.eclipse.jdt.ui.error", "org.eclipse.jdt.ui.warning", "org.eclipse.jdt.ui.info")
    
  final val SCRIPT_EXTENSION = ".sc"
    
  final val TAB_WIDTH = 2
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
