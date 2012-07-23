package org.scalaide.worksheet

import org.eclipse.core.resources.IFile
import scala.reflect.internal.util._

import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.util._

import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jface.text._

/** A Script compilation unit connects the presentation compiler
 *  view of a script with the Eclipse IDE view of the underlying
 *  resource.
 */
class ScriptCompilationUnit(val file: IFile) {

  private val scalaProject = ScalaPlugin.plugin.asScalaProject(file.getProject).get

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  def scriptSourceFile(contents: String): ScriptSourceFile = {
    ScriptSourceFile.apply(EclipseResource(file), contents.toCharArray)
  }

  /** Reconcile the unit. Return all compilation errors.
   *  Blocks until the unit is type-checked. 
   */
  def reconcile(newContents: String): List[IProblem] =
    scalaProject.withPresentationCompiler { pc =>
      val sourceFile = scriptSourceFile(newContents)
      pc.withResponse[Unit] { response =>
        pc.askReload(List(sourceFile), response)
        response.get
      }
      pc.problemsOf(sourceFile.file)
    }(null)
}

