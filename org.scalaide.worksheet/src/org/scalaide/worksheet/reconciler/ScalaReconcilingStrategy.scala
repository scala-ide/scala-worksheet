package org.scalaide.worksheet.reconciler

import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IDocumentListener
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.reconciler.DirtyRegion
import org.eclipse.jface.text.reconciler.IReconcilingStrategy
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension
import org.scalaide.logging.HasLogger
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.editor.ScriptEditor

class ScalaReconcilingStrategy(textEditor: ScriptEditor) extends IReconcilingStrategy with IReconcilingStrategyExtension with HasLogger {
  private var document: IDocument = _
  private lazy val scriptUnit = ScriptCompilationUnit.fromEditor(textEditor)

  override def setDocument(doc: IDocument) {
    document = doc

    doc.addDocumentListener(reloader)
  }

  override def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) {
    logger.debug("Incremental reconciliation not implemented.")
  }

  override def reconcile(partition: IRegion): Unit = try {
    val errors = scriptUnit.forceReconcile()

    textEditor.updateErrorAnnotations(errors)
  } catch {
    case e => eclipseLog.error("Exception while reconciling worksheet", e)
  }

  /** Ask the underlying unit to be scheduled for the next reconciliation round */
  private object reloader extends IDocumentListener {
    override def documentChanged(event: DocumentEvent) {
      scriptUnit.scheduleReconcile(document.get.toCharArray)
    }

    override def documentAboutToBeChanged(event: DocumentEvent) {}
  }

  def initialReconcile(): Unit = {
    scriptUnit.initialReconcile()
  }

  def setProgressMonitor(x$1: org.eclipse.core.runtime.IProgressMonitor): Unit = {}
}