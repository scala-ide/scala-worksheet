package org.scalaide.worksheet.wizards

import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.ui.dialogs.WizardNewFileCreationPage
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.io.InputStream
import java.io.ByteArrayInputStream

/**
 * Wizard page based of the new file creation page from the framework.
 * The advanced section has be removed.
 */
class NewWorksheetWizardPage(selection: IStructuredSelection) extends WizardNewFileCreationPage("main", selection) {

  // from org.eclipse.ui.dialogs.WizardNewFileCreationPage

  override def createAdvancedControls(parent: Composite) {
    // do nothing, we don't want the 'advanced' section
  }

  override def createLinkTarget() {
    // do nothing, we don't have this section
  }

  override def getInitialContents(): InputStream = {
    new ByteArrayInputStream(
      """object %s {
  println("Welcome to the Scala worksheet")
}"""
        .format(objectName).getBytes())
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

  /**
   * Return the name of the object to be created, or empty if not available.
   */
  def objectName = {
    val fileName = getFileName()
    if (fileName.length > 3) {
    	fileName.substring(0, fileName.length() - 3)
    } else {
      ""
    }
  }
  
  /**
   * check if the object name is a valid identifier.
   * TODO: switch to Scala identifier check. Right now it is checking for a Java identifier
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