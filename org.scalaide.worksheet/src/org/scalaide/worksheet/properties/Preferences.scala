package org.scalaide.worksheet.properties

import org.eclipse.jface.preference.BooleanFieldEditor
import org.eclipse.jface.preference.FieldEditorPreferencePage
import org.eclipse.jface.preference.IntegerFieldEditor
import org.eclipse.jface.preference.StringFieldEditor
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.scalaide.worksheet.WorksheetPlugin

class Preferences extends FieldEditorPreferencePage(FieldEditorPreferencePage.GRID) with IWorkbenchPreferencePage {
  import WorksheetPreferences._

  setPreferenceStore(WorksheetPlugin.plugin.getPreferenceStore)
  setDescription("""
Configure worksheet behavior.
  """)

  override def createFieldEditors() {
    val parent = getFieldEditorParent
    val evalOnSave = new BooleanFieldEditor(P_EVALUATE_ON_SAVE, "Evaluate worksheet on save", parent)
    evalOnSave.setEnabled(false, parent)
    addField(evalOnSave)
    addField(new IntegerFieldEditor(P_CUTOFF_VALUE, "Output character limit per statement", parent))
    addField(new StringFieldEditor(P_VM_ARGS, "Default VM arguments for worksheets", parent))
  }

  override def init(workbench: IWorkbench) {}

}

object WorksheetPreferences {
  val BASE = "org.scalaide.worksheet."
  val P_EVALUATE_ON_SAVE = BASE + "evalOnSave"
  val P_CUTOFF_VALUE = BASE + "cutoffValue"
  val P_VM_ARGS = BASE + "jvmArgs"
}
