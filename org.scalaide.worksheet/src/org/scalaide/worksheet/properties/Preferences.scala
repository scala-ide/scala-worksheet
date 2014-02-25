package org.scalaide.worksheet.properties

import org.scalaide.logging.HasLogger
import org.eclipse.jface.preference._
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.IWorkbench
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.ui.dialogs.PreferencesUtil
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
