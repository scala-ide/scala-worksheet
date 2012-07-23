package org.scalaide.worksheet.editors

import org.scalaide.worksheet.reconciler._

import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.eclipse.jface.text.reconciler._
import org.eclipse.jdt.ui.text.IJavaPartitions
import scala.tools.eclipse.lexical.ScalaPartitions
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import scala.tools.eclipse.ScalaDamagerRepairer
import org.eclipse.jface.text.presentation.PresentationReconciler
import org.eclipse.jface.text.source._
import scala.tools.eclipse.lexical._
import org.eclipse.jface.text.{IDocument, ITextHover, DefaultTextHover}
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jdt.internal.ui.JavaPlugin
import scala.tools.eclipse.ScalaPlugin
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector
import scala.tools.eclipse.ScalaHover
import scalariform.ScalaVersions
import scala.tools.eclipse.ScalaEditor
import org.eclipse.ui.texteditor.ITextEditor
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClasses

import org.eclipse.jdt.internal.ui.text.HTMLAnnotationHover

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
      override def isIncluded(a: Annotation): Boolean = annotationsShownInHover(a.getType)
    }
   
  }
  
  override def getTextHover(viewer: ISourceViewer, contentType: String): ITextHover = {
    return new DefaultTextHover(viewer)
  }
  
  private val scalaCodeScanner = new ScalaCodeScanner(javaColorManager, scalaPreferenceStore, ScalaVersions.DEFAULT)
  private val singleLineCommentScanner = new SingleTokenScanner(ScalaSyntaxClasses.SINGLE_LINE_COMMENT, javaColorManager, scalaPreferenceStore)
  private val multiLineCommentScanner = new SingleTokenScanner(ScalaSyntaxClasses.MULTI_LINE_COMMENT, javaColorManager, scalaPreferenceStore)
  private val scaladocScanner = new SingleTokenScanner(ScalaSyntaxClasses.SCALADOC, javaColorManager, scalaPreferenceStore)
  private val stringScanner = new SingleTokenScanner(ScalaSyntaxClasses.STRING, javaColorManager, scalaPreferenceStore)
  private val multiLineStringScanner = new SingleTokenScanner(ScalaSyntaxClasses.MULTI_LINE_STRING, javaColorManager, scalaPreferenceStore)
  private val xmlTagScanner = new XmlTagScanner(javaColorManager, scalaPreferenceStore)
  private val xmlCommentScanner = new XmlCommentScanner(javaColorManager, scalaPreferenceStore)
  private val xmlCDATAScanner = new XmlCDATAScanner(javaColorManager, scalaPreferenceStore)
  private val xmlPCDATAScanner = new SingleTokenScanner(ScalaSyntaxClasses.DEFAULT, javaColorManager, scalaPreferenceStore)
  private val xmlPIScanner = new XmlPIScanner(javaColorManager, scalaPreferenceStore)
  
  private val annotationsShownInHover = Set(
      "org.eclipse.jdt.ui.error", "org.eclipse.jdt.ui.warning", "org.eclipse.jdt.ui.info")

  //  override def getTextHover(sv: ISourceViewer, contentType: String, stateMask: Int) = {
  //    new ScalaHover(getCodeAssist _)
  //  }
  //
  //  override def getHyperlinkDetectors(sv: ISourceViewer): Array[IHyperlinkDetector] = {
  //    val detector = HyperlinksDetector()
  //    if (editor != null) detector.setContext(editor)
  //    Array(detector)
  //  }
}


object ScalaPartitioning {
  final val SCALA_PARTITIONING = "__scala_partitioning"
}
