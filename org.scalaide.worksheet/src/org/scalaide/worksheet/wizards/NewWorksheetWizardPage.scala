package org.scalaide.worksheet.wizards

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.dialogs.WizardNewFileCreationPage
import org.eclipse.core.runtime.IPath
import org.eclipse.core.resources.ResourcesPlugin
import org.scalaide.core.IScalaPlugin

/** Wizard page based of the new file creation page from the framework.
 *  The advanced section has be removed.
 */
class NewWorksheetWizardPage(selection: IStructuredSelection) extends WizardNewFileCreationPage("main", selection) {

  // from org.eclipse.ui.dialogs.WizardNewFileCreationPage

  override def createAdvancedControls(parent: Composite) {
    // do nothing, we don't want the 'advanced' section
  }

  override def createLinkTarget() {
    // do nothing, we don't have this section
  }

  override def getNewFileLabel(): String =
    "Worksheet name"

  /** Return the corresponding package name for a full (workspace-relative) path.
   *
   *  It works by checking the project source folders, seeing if any of them is a prefix
   *  of the given path and returning the rest.
   *
   *  It returns `None` if no source folder matches the given path, or the resulting
   *  path would be empty.
   */
  def packagePart(fullPath: IPath): Option[IPath] = {
    val project = ResourcesPlugin.getWorkspace().getRoot().getProject(fullPath.segment(0))

    (for {
      prj <- IScalaPlugin().asScalaProject(project).toSeq
      (sourceFolder, _) <- prj.sourceOutputFolders
      sourcePath = sourceFolder.getFullPath()
      if (sourcePath.isPrefixOf(fullPath))
      packagePath = fullPath.removeFirstSegments(sourcePath.segmentCount())
      if packagePath.segmentCount() > 0
    } yield packagePath) headOption
  }

  def pathToPackage(p: IPath): String = {
    p.segments() mkString "."
  }

  override def getInitialContents(): InputStream = {
    val prefix = packagePart(getContainerFullPath()).map(path => "package %s\n\n".format(pathToPackage(path))).getOrElse("")

    new ByteArrayInputStream(
      """%sobject %s {
  println("Welcome to the Scala worksheet")
}"""
        .format(prefix, objectName).getBytes())
  }

  override def validateLinkedResource(): IStatus = {
    // do nothing, we don't have this section
    Status.OK_STATUS
  }

  override def validatePage(): Boolean = {
    if (super.validatePage()) {
      if (validateIdentifier) {
        true
      } else {
        setErrorMessage("Invalid worksheet name")
        false
      }
    } else {
      true
    }
  }

  // ------

  // set the page values
  setTitle("Scala Worksheet")
  setDescription("Create a new Scala WorkSheet")
  setFileExtension("sc")

  /** Return the name of the object to be created, or empty if not available.
   */
  def objectName = {
    val fileName = getFileName()
    if (fileName.length > 3) {
      fileName.substring(0, fileName.length() - 3)
    } else {
      ""
    }
  }

  /** check if the object name is a valid identifier.
   *  TODO: switch to Scala identifier check. Right now it is checking for a Java identifier
   */
  def validateIdentifier: Boolean = {
    objectName.toList match {
      case Nil =>
        true
      case head :: Nil =>
        Character.isJavaIdentifierStart(head)
      case head :: tail =>
        Character.isJavaIdentifierStart(head) && tail.foldLeft(true)((b: Boolean, c: Char) => b && Character.isJavaIdentifierPart(c))
    }
  }

}