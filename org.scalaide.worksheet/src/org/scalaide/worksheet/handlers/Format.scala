package org.scalaide.worksheet.handlers

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.jface.text.ITextOperationTarget
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.ui.handlers.HandlerUtil
import org.scalaide.worksheet.editor.ScriptEditor

/**
 * Invoke the formatter on a Scala worksheet editor
 */
class Format extends AbstractHandler {

  override def execute(event: ExecutionEvent): AnyRef = {
    HandlerUtil.getActiveEditor(event) match {
      case editor: ScriptEditor =>
        // in our case, the viewer is always an ITexOperationTarget
        editor.getViewer.asInstanceOf[ITextOperationTarget].doOperation(ISourceViewer.FORMAT)
    }
    null
  }

}