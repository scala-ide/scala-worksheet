package org.scalaide.worksheet.properties

import org.scalaide.ui.syntax.preferences.BaseSyntaxColoringPreferencePage
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.lexical.SyntaxClasses


/**
 * @see org.eclipse.jdt.internal.ui.preferences.JavaEditorColoringConfigurationBlock
 */
class SyntaxColoringPreferencePage extends BaseSyntaxColoringPreferencePage(
    SyntaxClasses.categories,
    SyntaxClasses.resultCategory,
    WorksheetPlugin.plugin.getPreferenceStore,
    SyntaxColoringPreferencePage.previewText,
    PreviewerFactoryConfiguration)

object SyntaxColoringPreferencePage {
  val previewText = 
    """some code   //> Hello
      |some code   //| World
      |""".stripMargin
}
  
