package org.scalaide.worksheet.wizards

import org.eclipse.jface.wizard.Wizard
import org.eclipse.ui.INewWizard
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IWorkbench
import org.eclipse.jface.wizard.IWizardPage
import scala.tools.eclipse.util.SWTUtils
import org.eclipse.ui.IWorkbenchPage
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.PartInitException

/**
 * A wizard to create a new Scala worksheet file.
 */
class NewWorksheetWizard extends Wizard with INewWizard {

  // from org.eclipse.jface.wizard.Wizard

  override def performFinish(): Boolean = {
    val file = newFileWizardPage.createNewFile();
    
    if (file != null) {
      // if it worked, open the file
      SWTUtils.asyncExec {
        val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
          IDE.openEditor(page, file, true);
        } catch {
          case e: PartInitException =>
        }
      }
      true
    } else {
      false
    }
  }

  override def addPages() {
    newFileWizardPage = new NewWorksheetWizardPage(selection)
    addPage(newFileWizardPage)
  }

  // from org.eclipse.ui.INewWizard

  override def init(workbench: IWorkbench, selection: IStructuredSelection) {
    this.selection = selection
  }

  // ------
  
  // set the dialog values
  setWindowTitle("New Scala Worksheet")

  /**
   * The wizard page
   */
  private var newFileWizardPage: NewWorksheetWizardPage = _

  /**
   * The selection at the initialization of the wizard
   */
  private var selection: IStructuredSelection = _

}