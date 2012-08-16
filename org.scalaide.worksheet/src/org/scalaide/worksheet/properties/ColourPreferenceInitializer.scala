package org.scalaide.worksheet.properties

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.lexical.SyntaxClasses.EVAL_RESULT_FIRST_LINE
import org.scalaide.worksheet.lexical.SyntaxClasses.EVAL_RESULT_MARKER
import org.scalaide.worksheet.lexical.SyntaxClasses.EVAL_RESULT_NEW_LINE
import org.scalaide.worksheet.lexical.SyntaxClasses.EVAL_RESULT_DELIMITER

class ColourPreferenceInitializer extends AbstractPreferenceInitializer {

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
      val defaultBackgroundColour = StringConverter.asString(backgroundRGB)
      worksheetPrefStore.setDefault(syntaxClass.backgroundColourKey, defaultBackgroundColour)
      worksheetPrefStore.setDefault(syntaxClass.backgroundColourEnabledKey, true)
      worksheetPrefStore.setDefault(syntaxClass.boldKey, bold)
      worksheetPrefStore.setDefault(syntaxClass.italicKey, italic)
      worksheetPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

  private def setDefaultsForSyntaxClasses(implicit worksheetPrefStore: IPreferenceStore) {
    val editorBackground = Colors.White // currently the editor's background cannot be changed, so we can assume is white.
    setDefaultsForSyntaxClass(EVAL_RESULT_FIRST_LINE,  Colors.DarkGrey, backgroundRGB = editorBackground, italic = true)(worksheetPrefStore)
    setDefaultsForSyntaxClass(EVAL_RESULT_NEW_LINE, Colors.DarkGrey, backgroundRGB = editorBackground, italic = true)(worksheetPrefStore)
    setDefaultsForSyntaxClass(EVAL_RESULT_MARKER, editorBackground, backgroundRGB = editorBackground)(worksheetPrefStore)
    setDefaultsForSyntaxClass(EVAL_RESULT_DELIMITER, Colors.LightGreen, backgroundRGB = editorBackground)(worksheetPrefStore)
  }
}
