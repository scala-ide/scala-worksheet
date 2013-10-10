package org.scalaide.worksheet

import scala.tools.eclipse.InteractiveCompilationUnit
import scala.util.control.Exception.handling

import org.eclipse.core.resources.ResourcesPlugin

import org.eclipse.core.runtime.IPath

class SourceFileProvider extends scala.tools.eclipse.sourcefileprovider.SourceFileProvider {
  override def createFrom(path: IPath): Option[InteractiveCompilationUnit] = {
    val file = handling(classOf[Exception]).by(_ => None).apply {
      val root = ResourcesPlugin.getWorkspace().getRoot()
      Option(root.getFile(path))
    }
    
    file map (new ScriptCompilationUnit(_))
  }
}