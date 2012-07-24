package org.scalaide.worksheet

import org.eclipse.core.resources.IFile
import scala.tools.eclipse.ScalaProject
import org.eclipse.jdt.core.compiler.IProblem
import scala.reflect.internal.util.SourceFile

/** An abstraction over the IDE and Scala compiler views of a
 *  source file in a Scala project.
 *
 *  It may represent a script (no top-level declaration) or a compilation unit.
 */
trait InteractiveCompilationUnit {
  def file: IFile

  /** The Scala project to which this compilation unit belongs. */
  def scalaProject: ScalaProject

  /** Return a compiler `SourceFile` implementation. */
  def sourceFile(contents: String): SourceFile

  /** Reconcile the unit. Return all compilation errors.
   *
   *  Blocks until the unit is type-checked.
   */
  def reconcile(newContents: String): List[IProblem]
}
