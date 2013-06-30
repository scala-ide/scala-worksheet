package org.scalaide.worksheet.wizards

import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.util.SWTUtils
import scala.tools.eclipse.util.Utils

import org.eclipse.jface.viewers.IStructuredSelection

import org.eclipse.jface.wizard.Wizard
import org.eclipse.ui.INewWizard
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.PartInitException
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.scalaide.worksheet.editor.ScriptEditor

/**
 * A wizard to create a new Scala worksheet file.
 */
class NewWorksheetWizard extends Wizard with INewWizard with HasLogger {

  // from org.eclipse.jface.wizard.Wizard

  override def performFinish(): Boolean = {
    import Utils._
    val file = newFileWizardPage.createNewFile()
    
    if (file != null) {
      // if it worked, open the file
      SWTUtils.asyncExec {
        val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
        try {
          val editor = IDE.openEditor(page, file, true)
          editor.asInstanceOfOpt[ScriptEditor] foreach (_.runEvaluation)
        } catch {
          case e: PartInitException => eclipseLog.error("Failed to initialize editor for file "+ file.getName(), e)
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