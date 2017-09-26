package org.scalaide.worksheet

import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Platform
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.BundleEvent
import org.osgi.framework.BundleListener
import org.scalaide.logging.HasLogger
import org.scalaide.util.eclipse.OSGiUtils
import org.scalaide.worksheet.runtime.WorksheetsRuntime

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.osgi.ActorSystemActivator

object WorksheetPlugin extends HasLogger {
  @volatile var plugin: WorksheetPlugin = _
  private final val PluginId = "org.scalaide.worksheet"

  private final val worksheetBundle: Bundle = Platform.getBundle(WorksheetPlugin.PluginId)
  final val worksheetLibraries: Map[String, IPath] = {
    val worksheetRuntime212 = OSGiUtils.pathInBundle(worksheetBundle, "target/lib/worksheet-runtime-library_2.12.jar")
    val worksheetRuntime211 = OSGiUtils.pathInBundle(worksheetBundle, "target/lib/worksheet-runtime-library_2.11.jar")
    if(worksheetRuntime212.isEmpty)
      eclipseLog.error("The Scala Worksheet cannot be started correctly because worksheet runtime library for scala 2.12 is missing. Please report the issue.")
    if(worksheetRuntime211.isEmpty)
      eclipseLog.error("The Scala Worksheet cannot be started correctly because worksheet runtime library for scala 2.11 is missing. Please report the issue.")
    worksheetRuntime212.toSeq.map { "2.12" -> _ } ++ worksheetRuntime211.toSeq.map { "2.11" -> _ } toMap
  }

  def prefStore = plugin.getPreferenceStore

  def getImageDescriptor(path: String) = {
    AbstractUIPlugin.imageDescriptorFromPlugin(PluginId, path);
  }
}

class WorksheetPlugin extends AbstractUIPlugin {

  @volatile private var bundleListener: BundleListener = _
  @volatile var runtime: ActorRef = _
  
  class WorksheetActorSystemActivator extends ActorSystemActivator {
    override def configure(context: BundleContext, system: ActorSystem): Unit = {
      runtime = system.actorOf(WorksheetsRuntime.props, WorksheetsRuntime.ActorName)
    }
  }

  private val system = new WorksheetActorSystemActivator

  override def start(context: BundleContext) = {
    super.start(context)
    bundleListener = new BundleListener() {
      override def bundleChanged(event: BundleEvent) {
        if (event.getBundle() == getBundle()) {
          if (event.getType() == BundleEvent.STARTED)
            system.start(context)
        }
      }
    }
    context.addBundleListener(bundleListener)

    WorksheetPlugin.plugin = this
  }

  override def stop(context: BundleContext) = {
    WorksheetPlugin.plugin = null
    system.stop(context)
    if (bundleListener != null)
      context.removeBundleListener(bundleListener)
    super.stop(context)
  }
}