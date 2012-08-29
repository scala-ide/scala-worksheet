package org.scalaide.worksheet.editor

import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.formatter.ScalaFormattingStrategy
import scala.tools.eclipse.hyperlink.text.detector.DeclarationHyperlinkDetector
import scala.tools.eclipse.lexical.ScalaCodeScanner
import scala.tools.eclipse.lexical.ScalaPartitions
import scala.tools.eclipse.lexical.SingleTokenScanner
import scala.tools.eclipse.lexical.XmlCDATAScanner
import scala.tools.eclipse.lexical.XmlCommentScanner
import scala.tools.eclipse.lexical.XmlPIScanner
import scala.tools.eclipse.lexical.XmlTagScanner
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClasses
import scala.tools.eclipse.ui.AutoCloseBracketStrategy

import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy
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
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.ui.texteditor.ITextEditor
import org.scalaide.worksheet.completion.CompletionProposalComputer
import org.scalaide.worksheet.lexical.SingleLineCommentScanner
import org.scalaide.worksheet.reconciler.ScalaReconcilingStrategy

import scalariform.ScalaVersions

class ScriptConfiguration(val pluginPreferenceStore: IPreferenceStore, textEditor: ITextEditor) extends SourceViewerConfiguration {
  @inline private def scalaPreferenceStore: IPreferenceStore = ScalaPlugin.prefStore
  
  private val javaColorManager = JavaPlugin.getDefault.getJavaTextTools.getColorManager
  private val codeScanner = new ScalaCodeScanner(javaColorManager, scalaPreferenceStore, ScalaVersions.DEFAULT)

  override def getPresentationReconciler(sv: ISourceViewer) = {
    val reconciler = super.getPresentationReconciler(sv).asInstanceOf[PresentationReconciler]

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
    val reconciler = new MonoReconciler(new ScalaReconcilingStrategy(textEditor), /*isIncremental = */ false)
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
  private val singleLineCommentScanner = new SingleLineCommentScanner(scalaPreferenceStore, pluginPreferenceStore)
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
    val detector = DeclarationHyperlinkDetector()
    detector.setContext(textEditor)
    Array(detector)
  }

  def handlePropertyChangeEvent(event: PropertyChangeEvent) {
    scalaCodeScanner.adaptToPreferenceChange(event)
    scaladocScanner.adaptToPreferenceChange(event)
    stringScanner.adaptToPreferenceChange(event)
    multiLineStringScanner.adaptToPreferenceChange(event)
    singleLineCommentScanner.adaptToPreferenceChange(event)
    multiLineCommentScanner.adaptToPreferenceChange(event)
    xmlTagScanner.adaptToPreferenceChange(event)
    xmlCommentScanner.adaptToPreferenceChange(event)
    xmlCDATAScanner.adaptToPreferenceChange(event)
    xmlPCDATAScanner.adaptToPreferenceChange(event)
    xmlPIScanner.adaptToPreferenceChange(event)
  }

  override def getAutoEditStrategies(sourceViewer: ISourceViewer, contentType: String): Array[org.eclipse.jface.text.IAutoEditStrategy] = {
    val partitioning = getConfiguredDocumentPartitioning(sourceViewer)
    contentType match {
      // TODO: see why no jdt provided strategy is working
//      case IJavaPartitions.JAVA_DOC | IJavaPartitions.JAVA_MULTI_LINE_COMMENT =>
//        Array(new JavaDocAutoIndentStrategy(partitioning))
//      case IJavaPartitions.JAVA_STRING =>
//        Array(new SmartSemicolonAutoEditStrategy(partitioning), new JavaStringAutoIndentStrategy(partitioning))
//      case IJavaPartitions.JAVA_CHARACTER | IDocument.DEFAULT_CONTENT_TYPE =>
//        Array(new AutoCloseBracketStrategy(), new DefaultIndentLineAutoEditStrategy())
      case _ =>
        Array(new AutoCloseBracketStrategy(), new DefaultIndentLineAutoEditStrategy(), new EvaluationResultsAutoEditStrategy())
//        Array(new org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy())
    }
  }

}

object ScalaPartitioning {
  final val SCALA_PARTITIONING = "__scala_partitioning"
}
