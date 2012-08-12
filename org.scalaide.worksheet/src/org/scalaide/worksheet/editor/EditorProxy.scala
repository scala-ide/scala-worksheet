package org.scalaide.worksheet.editor

trait EditorProxy {
  def getContent: String
  def replaceWith(text: String, newCaretOffset: Int): Unit
  def caretOffset: Int
  def completedExternalEditorUpdate(): Unit
}