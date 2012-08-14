package org.scalaide.worksheet.properties

import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClass
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses.EVAL_RESULT_FIRST_LINE
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses.EVAL_RESULT_MARKER
import org.scalaide.worksheet.lexical.WorksheetSyntaxClasses.EVAL_RESULT_NEW_LINE
import scala.tools.eclipse.properties.syntaxcolouring.ColourPreferenceInitializer

class WorksheetColourPreferenceInitializer extends ColourPreferenceInitializer {

  override def initializeDefaultPreferences() {
    super.initializeDefaultPreferences
    doInitializeDefaultPreferences1()
  }

  private def doInitializeDefaultPreferences1() {
    setDefaultsForSyntaxClasses1(ScalaPlugin.prefStore)
  }

  private def setDefaultsForSyntaxClass1(
    syntaxClass: ScalaSyntaxClass,
    foregroundRGB: RGB,
    enabled: Boolean = true,
    backgroundRGBOpt: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false)(implicit scalaPrefStore: IPreferenceStore) =
    {
      lazy val WHITE = new RGB(255, 255, 255)
      scalaPrefStore.setDefault(syntaxClass.enabledKey, enabled)
      scalaPrefStore.setDefault(syntaxClass.foregroundColourKey, StringConverter.asString(foregroundRGB))
      val defaultBackgroundColour = StringConverter.asString(backgroundRGBOpt getOrElse WHITE)
      scalaPrefStore.setDefault(syntaxClass.backgroundColourKey, defaultBackgroundColour)
      scalaPrefStore.setDefault(syntaxClass.backgroundColourEnabledKey, backgroundRGBOpt.isDefined)
      scalaPrefStore.setDefault(syntaxClass.boldKey, bold)
      scalaPrefStore.setDefault(syntaxClass.italicKey, italic)
      scalaPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

  private def setDefaultsForSyntaxClasses1(implicit scalaPrefStore: IPreferenceStore) {
    val bgColor = new RGB(60, 60, 60)
    setDefaultsForSyntaxClass1(EVAL_RESULT_FIRST_LINE, new RGB(63, 127, 95), backgroundRGBOpt = Some(bgColor))(scalaPrefStore)
    setDefaultsForSyntaxClass1(EVAL_RESULT_NEW_LINE, new RGB(0, 192, 127), backgroundRGBOpt = Some(bgColor))(scalaPrefStore)
    setDefaultsForSyntaxClass1(EVAL_RESULT_MARKER, new RGB(0, 192, 127))(scalaPrefStore)
  }

}
