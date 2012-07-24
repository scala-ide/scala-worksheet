package org.scalaide.worksheet

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.part.FileEditorInput
import org.scalaide.worksheet.editors.ScriptEditor

import scala.reflect.internal.util.ScriptSourceFile
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.util.EclipseResource

/** A Script compilation unit connects the presentation compiler
 *  view of a script with the Eclipse IDE view of the underlying
 *  resource.
 */
case class ScriptCompilationUnit(val file: IFile) extends InteractiveCompilationUnit {

  def scalaProject = ScalaPlugin.plugin.asScalaProject(file.getProject).get

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  def sourceFile(contents: String): ScriptSourceFile = {
    ScriptSourceFile.apply(EclipseResource(file), contents.toCharArray)
  }

  /** Reconcile the unit. Return all compilation errors.
   *  Blocks until the unit is type-checked.
   */
  def reconcile(newContents: String): List[IProblem] =
    scalaProject.withPresentationCompiler { pc =>
      val src = sourceFile(newContents)
      pc.withResponse[Unit] { response =>
        pc.askReload(List(src), response)
        response.get
      }
      pc.problemsOf(src.file)
    }(null)
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