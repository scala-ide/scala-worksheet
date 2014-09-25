package org.scalaide.worksheet.editor

import org.scalaide.core.IScalaPlugin
import org.scalaide.core.internal.formatter.ScalaFormattingStrategy
import org.scalaide.core.hyperlink.detector.DeclarationHyperlinkDetector
import org.scalaide.ui.syntax.ScalaSyntaxClasses
import org.scalaide.ui.internal.editor.autoedits.BracketAutoEditStrategy
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
import org.scalaide.ui.internal.editor.hover.ScalaHover
import org.scalaide.worksheet.ScriptCompilationUnit
import org.eclipse.jface.util.IPropertyChangeListener
import org.scalaide.core.lexical.ScalaCodeScanners
import org.scalaide.core.lexical.ScalaPartitions

class ScriptConfiguration(val pluginPreferenceStore: IPreferenceStore, javaPreferenceStore: IPreferenceStore, textEditor: ScriptEditor) extends SourceViewerConfiguration with IPropertyChangeListener {
  @inline private def scalaPreferenceStore: IPreferenceStore = IScalaPlugin().getPreferenceStore()

  override def getPresentationReconciler(sv: ISourceViewer) = {
    val reconciler = super.getPresentationReconciler(sv).asInstanceOf[PresentationReconciler]

    for ((partitionType, tokenScanner) <- codeHighlightingScanners) {
      val dr = new DefaultDamagerRepairer(tokenScanner)
      reconciler.setDamager(dr, partitionType)
      reconciler.setRepairer(dr, partitionType)
    }

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
    new ScalaHover(ScriptCompilationUnit.fromEditor(textEditor))
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

  private val codeHighlightingScanners = ScalaCodeScanners.codeHighlightingScanners(scalaPreferenceStore, javaPreferenceStore)

  override def getHyperlinkDetectors(sv: ISourceViewer): Array[IHyperlinkDetector] = {
    val detector = DeclarationHyperlinkDetector()
    detector.setContext(textEditor)
    Array(detector)
  }

  def propertyChange(event: PropertyChangeEvent) {
    codeHighlightingScanners.values.foreach(_.adaptToPreferenceChange(event))
  }

  override def getAutoEditStrategies(sourceViewer: ISourceViewer, contentType: String): Array[org.eclipse.jface.text.IAutoEditStrategy] = {
    contentType match {
      // TODO: see why no jdt provided strategy is working
//      case IJavaPartitions.JAVA_DOC | IJavaPartitions.JAVA_MULTI_LINE_COMMENT =>
//        Array(new JavaDocAutoIndentStrategy(partitioning))
//      case IJavaPartitions.JAVA_STRING =>
//        Array(new SmartSemicolonAutoEditStrategy(partitioning), new JavaStringAutoIndentStrategy(partitioning))
//      case IJavaPartitions.JAVA_CHARACTER | IDocument.DEFAULT_CONTENT_TYPE =>
//        Array(new AutoCloseBracketStrategy(), new DefaultIndentLineAutoEditStrategy())
      case _ =>
        Array(new BracketAutoEditStrategy(scalaPreferenceStore), new DefaultIndentLineAutoEditStrategy(), new EvaluationResultsAutoEditStrategy())
//        Array(new org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy())
    }
  }

}

object ScalaPartitioning {
  final val SCALA_PARTITIONING = "__scala_partitioning"
}
