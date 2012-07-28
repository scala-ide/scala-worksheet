package org.scalaide.worksheet

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.part.FileEditorInput
import org.scalaide.worksheet.editors.ScriptEditor
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.ScriptSourceFile
import scala.tools.eclipse.InteractiveCompilationUnit
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.util.EclipseResource
import scala.tools.nsc.interactive.Response
import scala.tools.nsc.io.AbstractFile
import org.eclipse.jface.text.IDocument

/** A Script compilation unit connects the presentation compiler
 *  view of a script with the Eclipse IDE view of the underlying
 *  resource.
 */
case class ScriptCompilationUnit(val workspaceFile: IFile) extends InteractiveCompilationUnit {
  
  private var document: Option[IDocument] = None

  def file: AbstractFile = EclipseResource(workspaceFile)

  def scalaProject = ScalaPlugin.plugin.asScalaProject(workspaceFile.getProject).get

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  def sourceFile(contents: Array[Char]): ScriptSourceFile = {
    ScriptSourceFile.apply(file, contents)
  }

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  def batchSourceFile(contents: Array[Char]): BatchSourceFile = {
    new BatchSourceFile(file, contents)
  }

  def exists(): Boolean = true

  def getContents: Array[Char] = document.map(_.get.toCharArray).getOrElse(file.toCharArray)

  /** no-op */
  def scheduleReconcile(): Response[Unit] = {
    val r = new Response[Unit]
    r.set()
    r
  }

  def connect(doc: IDocument) {
    document = Option(doc)
  }
  
  def currentProblems: List[IProblem] = {
    scalaProject.withPresentationCompiler { pc =>
      pc.problemsOf(file)
    }(Nil)
  }

  /** Reconcile the unit. Return all compilation errors.
   *  Blocks until the unit is type-checked.
   */
  def reconcile(newContents: String): List[IProblem] =
    scalaProject.withPresentationCompiler { pc =>
      val src = batchSourceFile(newContents.toCharArray)
      pc.withResponse[Unit] { response =>
        pc.askReload(List(src), response)
        response.get
      }
      pc.problemsOf(src.file)
    }(Nil)
}

object ScriptCompilationUnit {
  def fromEditorInput(editorInput: IEditorInput): Option[ScriptCompilationUnit] = {
    getFile(editorInput).map(ScriptCompilationUnit.apply)
  }

  private def getFile(editorInput: IEditorInput): Option[IFile] =
    editorInput match {
      case fileEditorInput: FileEditorInput if fileEditorInput.getName.endsWith(ScriptEditor.SCRIPT_EXTENSION) =>
        Some(fileEditorInput.getFile)
      case _ => None
    }
}
