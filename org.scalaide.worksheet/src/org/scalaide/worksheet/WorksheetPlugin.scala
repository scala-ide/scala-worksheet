package org.scalaide.worksheet

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext
import org.scalaide.util.internal.eclipse.OSGiUtils.pathInBundle
import org.eclipse.core.runtime.Platform
import org.osgi.framework.Bundle
import org.scalaide.util.internal.Utils.WithAsInstanceOfOpt
import org.eclipse.core.runtime.IPath
import org.scalaide.logging.HasLogger
import org.eclipse.core.runtime.Path
import org.scalaide.worksheet.properties.WorksheetPreferences
import org.eclipse.core.resources.IProject

object WorksheetPlugin extends HasLogger {
  @volatile var plugin: WorksheetPlugin = _
  private final val PluginId = "org.scalaide.worksheet"

  private final val worksheetBundle: Bundle = Platform.getBundle(WorksheetPlugin.PluginId)
  final val worksheetLibrary: Option[IPath] = {
    val path2lib = pathInBundle(worksheetBundle, "target/lib/worksheet-runtime-library.jar")
    if(path2lib.isEmpty)
      eclipseLog.error("The Scala Worksheet cannot be started correctly because worksheet runtime library is missing. Please report the issue.")

    path2lib
  }


  def prefStore = plugin.getPreferenceStore

  def getImageDescriptor(path: String) = {
    AbstractUIPlugin.imageDescriptorFromPlugin(PluginId, path);
  }
}

class WorksheetPlugin extends AbstractUIPlugin {

  override def start(context: BundleContext) = {
    super.start(context)
    WorksheetPlugin.plugin = this
  }

  override def stop(context: BundleContext) = {
    WorksheetPlugin.plugin = null
    super.stop(context)
  }
}