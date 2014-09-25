package org.scalaide.worksheet.properties

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.IDocumentPartitioner
import org.eclipse.jface.text.ITextHover
import org.eclipse.jface.text.source.ISourceViewer
import org.scalaide.core.lexical.ScalaCodePartitioner
import org.scalaide.worksheet.editor.ScalaPartitioning
import org.scalaide.worksheet.editor.ScriptConfiguration

object PreviewerFactoryConfiguration extends org.scalaide.ui.syntax.preferences.PreviewerFactoryConfiguration {

  def getConfiguration(preferenceStore: IPreferenceStore) =
    new ScriptConfiguration(preferenceStore, preferenceStore, null) {
      override def getHyperlinkDetectors(sv: ISourceViewer) = Array()
      override def getTextHover(viewer: ISourceViewer, contentType: String): ITextHover = null
    } // anonymous class added to solve problem with getHyperlinkDetectors

  def getDocumentPartitioners(): Map[String, IDocumentPartitioner] = 
    Map((ScalaPartitioning.SCALA_PARTITIONING, ScalaCodePartitioner.documentPartitioner(conservative = true)))

}