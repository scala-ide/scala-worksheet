package org.scalaide.worksheet.editor

import scala.tools.eclipse.lexical.ScalaDocumentPartitioner

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
        val partitioner = new ScalaDocumentPartitioner
        docExt.setDocumentPartitioner(ScalaPartitioning.SCALA_PARTITIONING, partitioner)
        partitioner.connect(doc)
    }

    doc
  }

}