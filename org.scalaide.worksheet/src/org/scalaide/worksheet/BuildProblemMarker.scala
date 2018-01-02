package org.scalaide.worksheet

import org.eclipse.core.internal.resources.ResourceException
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor
import org.scalaide.core.resources.MarkerFactory
import org.scalaide.logging.HasLogger
import org.scalaide.util.ui.DisplayThread

object WorksheetMarkers extends HasLogger {
  val ProblemMarkerId = "org.scalaide.worksheet.problem"
  /**
   * Factory for creating markers used to report build problems (i.e., compilation errors).
   * Actually it is copy of [[org.scalaide.core.internal.builder.BuildProblemMarker]] with
   * changed marker ID.
   */
  private[worksheet] object BuildProblemMarker extends MarkerFactory(ProblemMarkerId)
  /**
   * Removes all problem markers from this IFile.
   */
  private[worksheet] def clearBuildErrors(file: IFile, monitor: IProgressMonitor) =
    try {
      DisplayThread.asyncExec {
        file.deleteMarkers(ProblemMarkerId, true, IResource.DEPTH_INFINITE)
      }
    } catch {
      case r: ResourceException =>
        logger.warn(r.getMessage)
    }
}
