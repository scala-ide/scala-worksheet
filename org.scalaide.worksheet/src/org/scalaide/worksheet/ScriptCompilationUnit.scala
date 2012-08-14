package org.scalaide.worksheet

import scala.tools.eclipse.InteractiveCompilationUnit
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.buildmanager.BuildProblemMarker
import scala.tools.eclipse.util.EclipseResource
import scala.tools.eclipse.util.FileUtils
import scala.tools.nsc.interactive.Response
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.BatchSourceFile
import scala.tools.nsc.util.Position
import scala.tools.nsc.util.ScriptSourceFile

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jface.text.IDocument
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.part.FileEditorInput
import org.eclipse.ui.texteditor.ITextEditor
import org.scalaide.worksheet.editor.ScriptEditor


/** A Script compilation unit connects the presentation compiler
 *  view of a script with the Eclipse IDE view of the underlying
 *  resource.
 */
case class ScriptCompilationUnit(val workspaceFile: IFile) extends InteractiveCompilationUnit {

  @volatile private var document: Option[IDocument] = None

  override def file: AbstractFile = EclipseResource(workspaceFile)

  override lazy val scalaProject = ScalaPlugin.plugin.asScalaProject(workspaceFile.getProject).get

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  override def sourceFile(contents: Array[Char]): ScriptSourceFile = {
    ScriptSourceFile.apply(file, contents)
  }

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  def batchSourceFile(contents: Array[Char]): BatchSourceFile = {
    new BatchSourceFile(file, contents)
  }

  override def exists(): Boolean = true

  override def getContents: Array[Char] = document.map(_.get.toCharArray).getOrElse(file.toCharArray)

  /** no-op */
  override def scheduleReconcile(): Response[Unit] = {
    val r = new Response[Unit]
    r.set()
    r
  }

  def connect(doc: IDocument): this.type = {
    document = Option(doc)
    this
  }

  override def currentProblems: List[IProblem] = {
    scalaProject.withPresentationCompiler { pc =>
      pc.problemsOf(file)
    }(Nil)
  }

  /** Reconcile the unit. Return all compilation errors.
   *  Blocks until the unit is type-checked.
   */
  override def reconcile(newContents: String): List[IProblem] =
    scalaProject.withPresentationCompiler { pc =>
      askReload(newContents.toCharArray)
      pc.problemsOf(file)
    }(Nil)

  def askReload(newContents: Array[Char] = getContents): Unit =
    scalaProject.withPresentationCompiler { pc =>
      val src = batchSourceFile(newContents)
      pc.withResponse[Unit] { response =>
        pc.askReload(List(src), response)
        response.get
      }
    }()
    
  def clearBuildErrors(): Unit = {
    FileUtils.clearBuildErrors(workspaceFile, null)
  }
  
  def reportBuildError(errorMsg: String, position: Position): Unit = {
    BuildProblemMarker.create(workspaceFile, IMarker.SEVERITY_ERROR, errorMsg, position)
  }
}

object ScriptCompilationUnit {
  def fromEditorInput(editorInput: IEditorInput): Option[ScriptCompilationUnit] = {
    getFile(editorInput).map(ScriptCompilationUnit.apply)
  }

  def fromEditor(textEditor: ITextEditor): Option[ScriptCompilationUnit] = {
    val input = textEditor.getEditorInput
    for (unit <- fromEditorInput(input))
      yield unit.connect(textEditor.getDocumentProvider().getDocument(input))
  }

  private def getFile(editorInput: IEditorInput): Option[IFile] =
    editorInput match {
      case fileEditorInput: FileEditorInput if fileEditorInput.getName.endsWith(ScriptEditor.SCRIPT_EXTENSION) =>
        Some(fileEditorInput.getFile)
      case _ => None
    }
}
