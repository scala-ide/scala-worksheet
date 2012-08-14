package org.scalaide.worksheet.properties

import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses.EVAL_RESULT_FIRST_LINE
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses.EVAL_RESULT_MARKER
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses.EVAL_RESULT_NEW_LINE
import org.scalaide.worksheet.WorksheetPlugin

class WorksheetColourPreferenceInitializer extends AbstractPreferenceInitializer {

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
    backgroundRGBOpt: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false)(implicit worksheetPrefStore: IPreferenceStore) =
    {
      lazy val WHITE = new RGB(255, 255, 255)
      worksheetPrefStore.setDefault(syntaxClass.enabledKey, enabled)
      worksheetPrefStore.setDefault(syntaxClass.foregroundColourKey, StringConverter.asString(foregroundRGB))
      val defaultBackgroundColour = StringConverter.asString(backgroundRGBOpt getOrElse WHITE)
      worksheetPrefStore.setDefault(syntaxClass.backgroundColourKey, defaultBackgroundColour)
      worksheetPrefStore.setDefault(syntaxClass.backgroundColourEnabledKey, backgroundRGBOpt.isDefined)
      worksheetPrefStore.setDefault(syntaxClass.boldKey, bold)
      worksheetPrefStore.setDefault(syntaxClass.italicKey, italic)
      worksheetPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

  private def setDefaultsForSyntaxClasses(implicit worksheetPrefStore: IPreferenceStore) {
    val bgColor = new RGB(60, 60, 60)
    setDefaultsForSyntaxClass(EVAL_RESULT_FIRST_LINE, new RGB(63, 127, 95), backgroundRGBOpt = Some(bgColor))(worksheetPrefStore)
    setDefaultsForSyntaxClass(EVAL_RESULT_NEW_LINE, new RGB(0, 192, 127), backgroundRGBOpt = Some(bgColor))(worksheetPrefStore)
    setDefaultsForSyntaxClass(EVAL_RESULT_MARKER, new RGB(0, 192, 127))(worksheetPrefStore)
  }

}
