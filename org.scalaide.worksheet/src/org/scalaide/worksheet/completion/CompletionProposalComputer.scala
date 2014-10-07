package org.scalaide.worksheet.completion

import org.scalaide.core.compiler.IScalaPresentationCompiler
import org.scalaide.util.ScalaWordFinder
import org.scalaide.core.completion.ScalaCompletions
import org.scalaide.ui.completion.ScalaCompletionProposal
import org.scalaide.util.eclipse.EditorUtils
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
    EditorUtils.getEditorCompilationUnit(textEditor) match {
      case Some(scu: ScriptCompilationUnit) =>
        // TODO: Not sure if this is the best way. Maybe compilation units should always be connected to something..
        scu.connect(viewer.getDocument)
        val completions = findCompletions(viewer, offset, scu)
        completions.toArray
      case _ => Array()
    }
  }

  private def findCompletions(viewer: ITextViewer, position: Int, scu: ScriptCompilationUnit): List[ICompletionProposal] = {
    val region = ScalaWordFinder.findCompletionPoint(viewer.getDocument.get, position)

    val res = findCompletions(region, position, scu).sortBy(_.relevance).reverse

    res.map(ScalaCompletionProposal(_))
  }

  override def computeContextInformation(viewer: ITextViewer, offset: Int): Array[IContextInformation] = {
    null
  }
}