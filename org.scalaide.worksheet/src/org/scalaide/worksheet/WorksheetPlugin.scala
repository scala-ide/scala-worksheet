package org.scalaide.worksheet

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext

object WorksheetPlugin {
  @volatile var plugin: WorksheetPlugin = _
  private val PLUGIN_ID = "Worksheet Plugin"

  def prefStore = plugin.getPreferenceStore

  def getImageDescriptor(path: String) = {
    AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
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