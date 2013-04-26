package org.scalaide.worksheet.editor

import java.nio.charset.Charset

/** A document holder, such as the ScriptEditor.
 * 
 *  Implementers receive asynchronous updates through `replaceWith`, therefore
 *  it is important that implementations are thread safe. Each method may be
 *  called on a different thread, at different times.
 */
trait DocumentHolder {
  
  /** This method is guaranteed to be called before the first call to `replaceWith`. */
  def beginUpdate(): Unit
  
  /** Return the contents of the document. */
  def getContents: String

  /** Return the encoding used for `this` document. */
  def encoding: Charset
  
  /** Replace the contents with the new text, and optionally reveal the given offset in
   *  the UI, if there is one. This method call is guaranteed to come after `beginUpdate`
   *  and before `endUpdate`.
   *  
   *  This is used for long output during evaluation to scroll and always show the latest
   *  insertion point.
   */
  def replaceWith(text: String, revealOffset: Int = -1): Unit
  
  /** The caller will not send any new calls to `replaceWith`. It is safe to clean up any resources
   *  needed during evaluation. For instance, the editor will remove the key listener that it uses
   *  to allow the user to interrupt the evaluation.
   */
  def endUpdate(): Unit
}

object DocumentHandler {
  final val DefaultEncoding: Charset = Charset.forName("UTF-8")
}