package org.scalaide.worksheet.editor.action

import org.eclipse.jface.action.Action
import org.scalaide.worksheet.editor.ScriptEditor

class RunEvaluationAction(editor: ScriptEditor) extends Action {
  setText("Evaluate")
  setDescription("Evaluate Worksheet")
  setToolTipText("Evaluate Worksheet")
  override def run(): Unit = editor.runEvaluation()
}