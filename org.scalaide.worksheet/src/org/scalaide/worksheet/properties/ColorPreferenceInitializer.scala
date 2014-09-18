package org.scalaide.worksheet.properties

import org.scalaide.ui.syntax.ScalaSyntaxClass

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.lexical.SyntaxClasses

class ColorPreferenceInitializer extends AbstractPreferenceInitializer {

  override def initializeDefaultPreferences() {
    doInitializeDefaultPreferences()
  }

  private def doInitializeDefaultPreferences() {
    setDefaultsForSyntaxClasses(WorksheetPlugin.prefStore)
  }

  private def setDefaultsForSyntaxClass(
    syntaxClass: ScalaSyntaxClass, 
    foregroundRGB: RGB, 
    enabled: Boolean = true, 
    backgroundRGB: RGB, 
    bold: Boolean = false, 
    italic: Boolean = false, 
    strikethrough: Boolean = false, 
    underline: Boolean = false)(implicit worksheetPrefStore: IPreferenceStore) =
    {
      worksheetPrefStore.setDefault(syntaxClass.enabledKey, enabled)
      worksheetPrefStore.setDefault(syntaxClass.foregroundColourKey, StringConverter.asString(foregroundRGB))
      val defaultBackgroundColor = StringConverter.asString(backgroundRGB)
      worksheetPrefStore.setDefault(syntaxClass.backgroundColourKey, defaultBackgroundColor)
      worksheetPrefStore.setDefault(syntaxClass.backgroundColourEnabledKey, true)
      worksheetPrefStore.setDefault(syntaxClass.boldKey, bold)
      worksheetPrefStore.setDefault(syntaxClass.italicKey, italic)
      worksheetPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

  private def setDefaultsForSyntaxClasses(implicit worksheetPrefStore: IPreferenceStore) {
    val editorBackground = Colors.White // currently the editor's background cannot be changed, so we can assume is white.
    setDefaultsForSyntaxClass(SyntaxClasses.FIRST_LINE,  Colors.DarkGrey, backgroundRGB = editorBackground, italic = true)(worksheetPrefStore)
    setDefaultsForSyntaxClass(SyntaxClasses.NEW_LINE, Colors.DarkGrey, backgroundRGB = editorBackground, italic = true)(worksheetPrefStore)
    setDefaultsForSyntaxClass(SyntaxClasses.MARKER, editorBackground, backgroundRGB = editorBackground)(worksheetPrefStore)
    setDefaultsForSyntaxClass(SyntaxClasses.DELIMITER, Colors.LightGreen, backgroundRGB = editorBackground)(worksheetPrefStore)
  }
}
