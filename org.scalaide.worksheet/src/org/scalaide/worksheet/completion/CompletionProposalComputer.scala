package org.scalaide.worksheet.completion

import org.scalaide.core.compiler.IScalaPresentationCompiler
import org.scalaide.util.ScalaWordFinder
import org.scalaide.core.completion.ScalaCompletions
import org.scalaide.ui.internal.completion.ScalaCompletionProposal
import org.scalaide.util.EditorUtils
import scala.reflect.internal.util.SourceFile

import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.IContentAssistProcessor
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.ui.texteditor.ITextEditor
import org.scalaide.worksheet.ScriptCompilationUnit

class CompletionProposalComputer(textEditor: ITextEditor) extends ScalaCompletions with IContentAssistProcessor {
  override def getCompletionProposalAutoActivationCharacters() = Array('.')

  override def getContextInformationAutoActivationCharacters() = Array[Char]()

  override def getErrorMessage = "No error"

  override def getContextInformationValidator = null

  override def computeCompletionProposals(viewer: ITextViewer, offset: Int): Array[ICompletionProposal] = {
    EditorUtils().getEditorCompilationUnit(textEditor) match {
      case Some(scu: ScriptCompilationUnit) =>
        // TODO: Not sure if this is the best way. Maybe compilation units should always be connected to something..
        scu.connect(viewer.getDocument)
        val completions = scu.withSourceFile { findCompletions(viewer, offset, scu) } getOrElse List[ICompletionProposal]()
        completions.toArray
      case _ => Array()
    }
  }

  private def findCompletions(viewer: ITextViewer, position: Int, scu: ScriptCompilationUnit)(sourceFile: SourceFile, compiler: IScalaPresentationCompiler): List[ICompletionProposal] = {
    val region = ScalaWordFinder().findCompletionPoint(viewer.getDocument.get, position)

    val res = findCompletions(region)(position, scu)(sourceFile, compiler).sortBy(_.relevance).reverse

    res.map(ScalaCompletionProposal.apply)
  }

  override def computeContextInformation(viewer: ITextViewer, offset: Int): Array[IContextInformation] = {
    null
  }
}