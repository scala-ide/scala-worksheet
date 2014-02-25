package org.scalaide.worksheet.editor

import org.scalaide.core.internal.lexical.ScalaDocumentPartitioner

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IDocumentExtension3
import org.eclipse.ui.editors.text.FileDocumentProvider

/** A Document provider for Scala scripts. It sets the Scala
 *  partitioner.
 */
class ScriptDocumentProvider extends FileDocumentProvider {

  override def createDocument(input: Object): IDocument = {
    val doc = super.createDocument(input)

    doc match {
      case docExt: IDocumentExtension3 =>
        // TODO: maybe it's not necessary to be conservative. This makes the partitioner
        // always return 'true' when asked about changed partitioning. Fixes #82, that
        // lost colorization when entering a new line between an expression and its result
        val partitioner = new ScalaDocumentPartitioner(conservative=true)
        docExt.setDocumentPartitioner(ScalaPartitioning.SCALA_PARTITIONING, partitioner)
        partitioner.connect(doc)
    }

    doc
  }

}