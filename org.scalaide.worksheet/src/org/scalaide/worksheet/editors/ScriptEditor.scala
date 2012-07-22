package org.scalaide.worksheet.editors

import org.eclipse.ui.IEditorInput
import org.eclipse.swt.graphics.Image
import scala.tools.eclipse.ScalaSourceFileEditor
import org.eclipse.ui.editors.text.TextEditor
import org.eclipse.jface.text.source.SourceViewerConfiguration
import scala.tools.eclipse.ScalaSourceViewerConfiguration
import scala.tools.eclipse.ScalaPlugin
import org.eclipse.jdt.internal.ui.JavaPlugin

class ScriptEditor extends TextEditor {

  setPartName("Scala Script Editor")
  setDocumentProvider(new ScriptDocumentProvider)

  def scalaPrefStore = ScalaPlugin.prefStore
  def javaPrefStore = JavaPlugin.getDefault.getPreferenceStore

  
  override def initializeEditor() {
    super.initializeEditor()
    setSourceViewerConfiguration(new ScriptConfiguration(this))
  }
}
