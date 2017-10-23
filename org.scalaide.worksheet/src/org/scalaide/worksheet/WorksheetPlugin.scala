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
    Seq("2.10", "2.11", "2.12").map { version =>
      val lib = OSGiUtils.pathInBundle(worksheetBundle, s"target/lib/worksheet-runtime-library_$version.jar")
      if (lib.isEmpty)
        eclipseLog.error(s"The Scala Worksheet cannot be started correctly because worksheet runtime library for scala $version is missing. Please report the issue.")
      version -> lib
    }.collect {
      case (version, Some(lib)) => version -> lib
    }.toMap
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