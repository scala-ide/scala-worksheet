package org.scalaide.worksheet
package reconciler

import org.eclipse.jface.text.reconciler._
import org.eclipse.jface.text._
import org.eclipse.jface.text.source._
import org.eclipse.ui.texteditor._
import org.eclipse.ui.part.FileEditorInput

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.compiler.IProblem

import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.{ ScalaPlugin, ScalaProject }

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.ProblemAnnotation

class ScalaReconcilingStrategy(textEditor: ITextEditor) extends IReconcilingStrategy with HasLogger {
  private var document: IDocument = _
  private lazy val annotationModel = textEditor.getDocumentProvider.getAnnotationModel(textEditor.getEditorInput)

  lazy val scriptUnit = new ScriptCompilationUnit(getFile)

  def setDocument(doc: IDocument) {
    document = doc
  }

  def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) {
    logger.debug("Incremental reconciliation not implemented.")
  }

  def reconcile(partition: IRegion) {
    logger.info("Reconciling full doc on " + document)
    val errors = scriptUnit.reconcile(document.get)

    updateErrorAnnotations(errors)
  }

  private def getFile: IFile =
    textEditor.getEditorInput match {
      case fileEditorInput: FileEditorInput =>
        fileEditorInput.getFile
    }

  private var previousAnnotations = List[ProblemAnnotation]()
  private var previousErrors = List[IProblem]()

  private def updateErrorAnnotations(errors: List[IProblem]) {
    def position(p: IProblem) = new Position(p.getSourceStart, p.getSourceEnd - p.getSourceStart + 1)

    previousAnnotations.foreach(annotationModel.removeAnnotation)

    for (e <- errors) {
      //      val annotation = new Annotation("org.scala-ide.sdt.core.problem", false, e.getMessage) // no compilation unit
      val annotation = new ProblemAnnotation(e, null) // no compilation unit
      annotationModel.addAnnotation(annotation, position(e))
      previousAnnotations ::= annotation
    }
  }
}


