package org.scalaide.worksheet

import org.eclipse.core.runtime.IPath
import scala.tools.eclipse.InteractiveCompilationUnit
import org.eclipse.ui.internal.Workbench
import org.eclipse.ui.IWorkbench
import org.eclipse.core.resources.ResourcesPlugin
import scala.util.control.Exception.handling

class SourceFileProvider extends scala.tools.eclipse.sourcefileprovider.SourceFileProvider {
  def createFrom(path: IPath): Option[InteractiveCompilationUnit] = {
    val file = handling(classOf[Exception]).by(_ => None).apply {
      val root = ResourcesPlugin.getWorkspace().getRoot()
      Option(root.getFile(path))
    }
    
    file map (new ScriptCompilationUnit(_))
  }
}