package org.scalaide.worksheet.editors

import org.eclipse.ui.editors.text.TextEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.ISelectionListener
import org.eclipse.swt.widgets.Composite

/** An editor trait that follows selection change events and calls `selectionChanged`.
 *  It encapsulates a selection listener, so clients don't need to take care of
 *  creating, installing and uninstalling a listener themselves.
 *
 *  Mix-in this trait and implement `selectionChanged` to get notified of selection changes.
 */
trait SelectionTracker extends TextEditor {

  /** Selection in this editor has changed. */
  def selectionChanged(selection: ITextSelection)

  lazy val selectionListener = new ISelectionListener() {
    def selectionChanged(part: IWorkbenchPart, selection: ISelection) {
      selection match {
        case textSel: ITextSelection => SelectionTracker.this.selectionChanged(textSel)
        case _                       =>
      }
    }
  }

  override def createPartControl(parent: Composite) {
    super.createPartControl(parent)
    getEditorSite.getPage.addPostSelectionListener(selectionListener)
  }

  override def dispose() {
    getEditorSite.getPage.removePostSelectionListener(selectionListener)
    super.dispose()
  }
}
