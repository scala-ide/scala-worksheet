package org.scalaide.worksheet.reconciler

import org.eclipse.jface.text.reconciler._
import org.eclipse.jface.text._
import org.eclipse.ui.texteditor._
import org.eclipse.ui.part.FileEditorInput

import org.eclipse.core.resources.IFile

import org.eclipse.jdt.core.compiler.IProblem

import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.{ ScalaPlugin, ScalaProject }
import scala.tools.eclipse.util._

import scala.reflect.internal.util._

class ScalaReconcilingStrategy(textEditor: ITextEditor) extends IReconcilingStrategy with HasLogger {
  private var document: IDocument = _

  def setDocument(doc: IDocument) {
    document = doc
  }

  def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) {
    logger.debug("Reconciling on " + document)
  }

  def reconcile(partition: IRegion) {
    logger.info("Reconciling full doc on " + document)
    val errors = compilationErrors(partition)
    
    updateErrorAnnotations(errors)
  }

  private def compilationErrors(region: IRegion): List[IProblem] =
    withScalaProject { prj =>
      prj.withPresentationCompiler { pc =>
        val sourceFile = scriptSourceFile(region)
        pc.withResponse[Unit] { response =>
          pc.askReload(List(sourceFile), response)
          response.get
        }
        pc.problemsOf(sourceFile.file)
      }(null)
    } getOrElse Nil

  def scriptSourceFile(region: IRegion): SourceFile = {
    val contents = document.get(region.getOffset, region.getLength)
    ScriptSourceFile.apply(EclipseResource(getFile), contents.toCharArray)
  }

  private def getFile: IFile =
    textEditor.getEditorInput match {
      case fileEditorInput: FileEditorInput =>
        fileEditorInput.getFile
    }

  private def withScalaProject[A](op: ScalaProject => A): Option[A] = {
    ScalaPlugin.plugin.asScalaProject(getFile.getProject) map op
  }
  
  private def updateErrorAnnotations(errors: List[IProblem]) {
    // TODO: update the annotation model
  }
}


