package org.scalaide.worksheet.editor

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IDocumentExtension3
import org.eclipse.ui.editors.text.FileDocumentProvider
import org.eclipse.ui.editors.text.TextFileDocumentProvider
import org.eclipse.ui.editors.text.ForwardingDocumentProvider
import org.eclipse.ui.texteditor.IDocumentProvider
import org.eclipse.core.filebuffers.IDocumentSetupParticipant
import org.scalaide.core.lexical.ScalaCodePartitioner

/** A Document provider for Scala scripts. It sets the Scala
 *  partitioner.
 */
class ScriptDocumentProvider extends TextFileDocumentProvider {

  val provider: IDocumentProvider = new TextFileDocumentProvider()
  val fwd = new ForwardingDocumentProvider(ScalaPartitioning.SCALA_PARTITIONING, new ScriptDocumentSetupParticipant, provider)
  setParentDocumentProvider(fwd)
}

class ScriptDocumentSetupParticipant extends IDocumentSetupParticipant {
  override def setup(doc: IDocument): Unit = {
    doc match {
      case docExt: IDocumentExtension3 =>
        // TODO: maybe it's not necessary to be conservative. This makes the partitioner
        // always return 'true' when asked about changed partitioning. Fixes #82, that
        // lost colorization when entering a new line between an expression and its result
        val partitioner = ScalaCodePartitioner.documentPartitioner(conservative=true)
        docExt.setDocumentPartitioner(ScalaPartitioning.SCALA_PARTITIONING, partitioner)
        partitioner.connect(doc)
    }
  }
}