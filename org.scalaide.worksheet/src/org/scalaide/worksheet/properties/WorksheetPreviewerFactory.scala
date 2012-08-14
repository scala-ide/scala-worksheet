package org.scalaide.worksheet.properties

import scala.tools.eclipse.lexical.ScalaDocumentPartitioner
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.text.source.SourceViewer
import org.eclipse.jface.text.source.projection.ProjectionViewer
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.ChainedPreferenceStore
import org.scalaide.worksheet.editor.ScriptConfiguration
import java.util.HashMap
import org.eclipse.jface.text.IDocumentPartitioner
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jface.text.TextUtilities
import org.scalaide.worksheet.editor.ScalaPartitioning

object WorksheetPreviewerFactory {

  def createPreviewer(parent: Composite, scalaPreferenceStore: IPreferenceStore, initialText: String): SourceViewer = {
    val preferenceStore = new ChainedPreferenceStore(Array(scalaPreferenceStore, EditorsUI.getPreferenceStore))
    val previewViewer = new ProjectionViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER)
    val font = JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT)
    previewViewer.getTextWidget.setFont(font)
    previewViewer.setEditable(false)

    val configuration = new ScriptConfiguration(preferenceStore, null) {
      override def getHyperlinkDetectors(sv: ISourceViewer) = Array()
    } // anonymous class added to solve problem with getHyperlinkDetectors
    previewViewer.configure(configuration)

    val document = new Document
    document.set(initialText)
    val partitioner = new ScalaDocumentPartitioner(conservative = true)
    partitioner.connect(document)
    document.setDocumentPartitioner(ScalaPartitioning.SCALA_PARTITIONING, partitioner)
    previewViewer.setDocument(document)
    previewViewer.invalidateTextPresentation()
    
//    val partitioners = new HashMap[String, IDocumentPartitioner]
//    partitioners.put(IJavaPartitions.JAVA_PARTITIONING, new ScalaDocumentPartitioner(conservative = true))
//    TextUtilities.addDocumentPartitioners(document, partitioners)
//    previewViewer.setDocument(document)

    preferenceStore.addPropertyChangeListener(new IPropertyChangeListener {
      def propertyChange(event: PropertyChangeEvent) {
        configuration.handlePropertyChangeEvent(event)
        previewViewer.invalidateTextPresentation()
      }
    })
    previewViewer
  }

}