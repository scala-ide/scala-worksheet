package org.scalaide.worksheet.reconciler

import org.scalaide.logging.HasLogger
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.ProblemAnnotation
import org.eclipse.jface.text._
import org.eclipse.jface.text.reconciler._
import org.eclipse.jface.text.source._
import org.eclipse.ui.texteditor._
import org.scalaide.worksheet.ScriptCompilationUnit
import scala.collection.JavaConverters
import org.scalaide.worksheet.editor.ScriptEditor
import org.eclipse.ltk.internal.ui.refactoring.util.SWTUtil
import org.scalaide.util.internal.eclipse.SWTUtils

class ScalaReconcilingStrategy(textEditor: ScriptEditor) extends IReconcilingStrategy with HasLogger {
  private var document: IDocument = _
  private lazy val scriptUnit = ScriptCompilationUnit.fromEditor(textEditor)

  override def setDocument(doc: IDocument) {
    document = doc

    doc.addDocumentListener(reloader)
  }

  override def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) {
    logger.debug("Incremental reconciliation not implemented.")
  }

  override def reconcile(partition: IRegion) {
    val errors = scriptUnit.reconcile(document.get)

    textEditor.updateErrorAnnotations(errors)
  }

  /** Ask the underlying unit to reload on each document change event.
   *
   *  This is certainly wasteful, but otherwise the AST trees are not up to date
   *  in the interval between the last keystroke and reconciliation (which has a delay of
   *  500ms usually). The user can be quick and ask for completions in this interval, and get
   *  wrong results.
   */
  private object reloader extends IDocumentListener {
    override def documentChanged(event: DocumentEvent) {
      scriptUnit.askReload()
    }

    override def documentAboutToBeChanged(event: DocumentEvent) {}
  }
}