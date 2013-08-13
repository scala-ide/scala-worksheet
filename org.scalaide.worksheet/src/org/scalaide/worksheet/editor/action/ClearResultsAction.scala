package org.scalaide.worksheet.editor.action

import org.eclipse.jface.action.Action
import org.scalaide.worksheet.editor.ScriptEditor

class ClearResultsAction(editor: ScriptEditor) extends Action {
  setText("Clear Results")
  setDescription("Clear Results in Worksheet")
  setToolTipText("Clear Results in Worksheet")
  override def run(): Unit = editor.clearResults()
}
