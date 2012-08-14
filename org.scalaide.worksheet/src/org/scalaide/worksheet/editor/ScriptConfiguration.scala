package org.scalaide.worksheet.editor

import scala.tools.eclipse.ScalaDamagerRepairer
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.formatter.ScalaFormattingStrategy
import scala.tools.eclipse.hyperlink.text.detector.HyperlinksDetector
import scala.tools.eclipse.lexical.ScalaCodeScanner
import scala.tools.eclipse.lexical.ScalaPartitions
import scala.tools.eclipse.lexical.SingleTokenScanner
import scala.tools.eclipse.lexical.XmlCDATAScanner
import scala.tools.eclipse.lexical.XmlCommentScanner
import scala.tools.eclipse.lexical.XmlPIScanner
import scala.tools.eclipse.lexical.XmlTagScanner
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClasses

import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jface.text.DefaultTextHover
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextHover
import org.eclipse.jface.text.contentassist.ContentAssistant
import org.eclipse.jface.text.contentassist.IContentAssistant
import org.eclipse.jface.text.formatter.MultiPassContentFormatter
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector
import org.eclipse.jface.text.presentation.PresentationReconciler
import org.eclipse.jface.text.reconciler.IReconciler
import org.eclipse.jface.text.reconciler.MonoReconciler
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.DefaultAnnotationHover
import org.eclipse.jface.text.source.IAnnotationHover
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.eclipse.ui.texteditor.ITextEditor
import org.scalaide.worksheet.completion.CompletionProposalComputer
import org.scalaide.worksheet.lexical.SingleLineCommentScanner
import org.scalaide.worksheet.reconciler.ScalaReconcilingStrategy

import scalariform.ScalaVersions

class ScriptConfiguration(textEditor: ITextEditor) extends SourceViewerConfiguration {
  private val scalaPreferenceStore = ScalaPlugin.plugin.getPreferenceStore

  val codeScanner = new ScalaCodeScanner(javaColorManager, scalaPreferenceStore, ScalaVersions.DEFAULT)

  val javaColorManager = JavaPlugin.getDefault.getJavaTextTools.getColorManager

  override def getPresentationReconciler(sv: ISourceViewer) = {
    val reconciler = super.getPresentationReconciler(sv).asInstanceOf[PresentationReconciler]
    val dr = new ScalaDamagerRepairer(codeScanner)

    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE)
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE)

    def handlePartition(partitionType: String, tokenScanner: ITokenScanner) {
      val dr = new DefaultDamagerRepairer(tokenScanner)
      reconciler.setDamager(dr, partitionType)
      reconciler.setRepairer(dr, partitionType)
    }

    handlePartition(IDocument.DEFAULT_CONTENT_TYPE, scalaCodeScanner)
    handlePartition(IJavaPartitions.JAVA_DOC, scaladocScanner)
    handlePartition(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT, singleLineCommentScanner)
    handlePartition(IJavaPartitions.JAVA_MULTI_LINE_COMMENT, multiLineCommentScanner)
    handlePartition(IJavaPartitions.JAVA_STRING, stringScanner)
    handlePartition(ScalaPartitions.SCALA_MULTI_LINE_STRING, multiLineStringScanner)
    handlePartition(ScalaPartitions.XML_TAG, xmlTagScanner)
    handlePartition(ScalaPartitions.XML_COMMENT, xmlCommentScanner)
    handlePartition(ScalaPartitions.XML_CDATA, xmlCDATAScanner)
    handlePartition(ScalaPartitions.XML_PCDATA, xmlPCDATAScanner)
    handlePartition(ScalaPartitions.XML_PI, xmlPIScanner)

    reconciler
  }

  override def getConfiguredDocumentPartitioning(sourceViewer: ISourceViewer): String = {
    ScalaPartitioning.SCALA_PARTITIONING
  }
  
  override def getConfiguredContentTypes(sourceViewer: ISourceViewer): Array[String] = {
    Array(IDocument.DEFAULT_CONTENT_TYPE,
      IJavaPartitions.JAVA_DOC,
      IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
      IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
      IJavaPartitions.JAVA_STRING,
      IJavaPartitions.JAVA_CHARACTER,
      ScalaPartitions.SCALA_MULTI_LINE_STRING)
  }
  
  override def getReconciler(sourceViewer: ISourceViewer): IReconciler = {
    val reconciler = new MonoReconciler(new ScalaReconcilingStrategy(textEditor), /*isIncremental = */false)
    reconciler.install(sourceViewer)
    reconciler
  }

  override def getAnnotationHover(viewer: ISourceViewer): IAnnotationHover = {
    new DefaultAnnotationHover(true) {
      override def isIncluded(a: Annotation): Boolean = ScriptEditor.annotationsShownInHover(a.getType)
    }
   
  }
  
  override def getTextHover(viewer: ISourceViewer, contentType: String): ITextHover = {
    return new DefaultTextHover(viewer)
  }

  override def getTabWidth(viewer: ISourceViewer): Int = ScriptEditor.TAB_WIDTH

  override def getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant = {
    val assistant = new ContentAssistant
    assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer))
    assistant.setContentAssistProcessor(new CompletionProposalComputer(textEditor), IDocument.DEFAULT_CONTENT_TYPE)
    assistant
  }
  
  override def getContentFormatter(viewer: ISourceViewer) = {
    val formatter = new MultiPassContentFormatter(getConfiguredDocumentPartitioning(viewer), IDocument.DEFAULT_CONTENT_TYPE)
    formatter.setMasterStrategy(new ScalaFormattingStrategy(textEditor))
    formatter
  }
  
  private val scalaCodeScanner = new ScalaCodeScanner(javaColorManager, scalaPreferenceStore, ScalaVersions.DEFAULT)
  private val singleLineCommentScanner = new SingleLineCommentScanner(javaColorManager, scalaPreferenceStore)
  private val multiLineCommentScanner = new SingleTokenScanner(ScalaSyntaxClasses.MULTI_LINE_COMMENT, javaColorManager, scalaPreferenceStore)
  private val scaladocScanner = new SingleTokenScanner(ScalaSyntaxClasses.SCALADOC, javaColorManager, scalaPreferenceStore)
  private val stringScanner = new SingleTokenScanner(ScalaSyntaxClasses.STRING, javaColorManager, scalaPreferenceStore)
  private val multiLineStringScanner = new SingleTokenScanner(ScalaSyntaxClasses.MULTI_LINE_STRING, javaColorManager, scalaPreferenceStore)
  private val xmlTagScanner = new XmlTagScanner(javaColorManager, scalaPreferenceStore)
  private val xmlCommentScanner = new XmlCommentScanner(javaColorManager, scalaPreferenceStore)
  private val xmlCDATAScanner = new XmlCDATAScanner(javaColorManager, scalaPreferenceStore)
  private val xmlPCDATAScanner = new SingleTokenScanner(ScalaSyntaxClasses.DEFAULT, javaColorManager, scalaPreferenceStore)
  private val xmlPIScanner = new XmlPIScanner(javaColorManager, scalaPreferenceStore)

  override def getHyperlinkDetectors(sv: ISourceViewer): Array[IHyperlinkDetector] = {
    // FIXME: only works with external targets
    val detector = HyperlinksDetector()
    detector.setContext(textEditor)
    Array(detector)
  }
}


object ScalaPartitioning {
  final val SCALA_PARTITIONING = "__scala_partitioning"
}
