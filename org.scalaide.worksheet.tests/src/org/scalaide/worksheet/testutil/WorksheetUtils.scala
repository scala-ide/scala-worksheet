package org.scalaide.worksheet.testutil

import org.scalaide.worksheet.ScriptCompilationUnit
import java.io.ByteArrayInputStream
import org.scalaide.core.testsetup.SDTTestUtils
import org.eclipse.core.runtime.Path
import org.eclipse.core.resources.IResource

object WorksheetUtils {
  import SDTTestUtils._
  
  /** Create a script with the given contenst.
   *  
   *  @param fullPath the full path relative to the workspace. It needs to include the
   *         project name.
   */
  def getUnit(fullPath: String, contents: String): ScriptCompilationUnit = {
    val bytes = new ByteArrayInputStream(contents.getBytes)

    val iFile = workspace.getRoot.getFile(new Path(fullPath))
    iFile.create(bytes, IResource.NONE, null)

    ScriptCompilationUnit(iFile)
  }
}
