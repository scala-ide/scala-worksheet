package org.scalaide.worksheet.properties

import org.scalaide.worksheet.WorksheetPlugin
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer

class WorksheetPreferenceInitializer extends AbstractPreferenceInitializer {

  import WorksheetPreferences._

  override def initializeDefaultPreferences() {
    val store = WorksheetPlugin.plugin.getPreferenceStore
    store.setDefault(P_EVALUATE_ON_SAVE, true)
    store.setDefault(P_CUTOFF_VALUE, 1000)
  }
}
