package org.scalaide.worksheet.handlers

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.ui.handlers.HandlerUtil
import org.eclipse.ui.texteditor.ITextEditor
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.eval.WorksheetEvaluator
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.logging.HasLogger
import scala.tools.nsc.scratchpad.SourceInserter
import scala.tools.eclipse.util.EditorUtils
import scala.tools.eclipse.refactoring.EditorHelpers
import org.eclipse.jface.text.IDocument
import scala.tools.nsc.reporters.StoreReporter
import org.scalaide.worksheet.eval.EvaluationError
import org.scalaide.worksheet.eval.InstrumentedError
import org.scalaide.worksheet.eval.CompilationError
import org.scalaide.worksheet.eval.InstrumentedError
import org.scalaide.worksheet.eval.ExecutionError
import scala.tools.nsc.scratchpad.Mixer

class EvalScript extends AbstractHandler with HasLogger {

  override def execute(event: ExecutionEvent): AnyRef = {
    for {
      editor <- Option(HandlerUtil.getActiveEditor(event).asInstanceOf[ITextEditor])
      editorInput <- Option(HandlerUtil.getActiveEditorInput(event))
      scriptUnit <- ScriptCompilationUnit.fromEditor(editor)
    } {
      val doc = editor.getDocumentProvider.getDocument(editorInput)
      evalDocument(scriptUnit, doc) match {
        case Left(CompilationError(reporter)) =>
          MessageDialog.openError(ScalaPlugin.getShell,
            "Error during evaluation",
            "Compilation errors during the evaluation of instrumented code\n(please open a bug report):\n\n" + reporter.infos.map(_.msg).mkString("\n"))

        case Left(InstrumentedError(ex)) =>
          eclipseLog.error("Error during askInstrumented", ex)

        case Left(ExecutionError(ex)) =>
          eclipseLog.debug("Error evaluating the worksheet", ex)

        case Right(result) =>
          logger.debug(result)
          if (result.length > 0) {
            val stripped = SourceInserter.stripRight(doc.get.toCharArray)
            val mixer = new Mixer
            doc.replace(0, doc.getLength, mixer.mix(stripped, result.toCharArray()).mkString)
          }
      }
    }

    null
  }

  override def isEnabled: Boolean =
    EditorHelpers.withCurrentEditor { editor =>
      EditorUtils.getEditorScalaInput(editor) map { scu => scu.currentProblems.isEmpty }
    } getOrElse false


  def evalDocument(scriptUnit: ScriptCompilationUnit, doc: IDocument): Either[EvaluationError, String] = {
    scriptUnit.scalaProject.withPresentationCompiler { compiler =>
      val source = scriptUnit.batchSourceFile(SourceInserter.stripRight(doc.get.toCharArray))
      compiler.withResponse[Unit] { compiler.askReload(List(source), _) } // just make sure it's loaded
      compiler.withResponse[(String, Array[Char])] { compiler.askInstrumented(source, -1, _) }.get
    }() match {
      case Left((fullName, instrumented)) =>
        logger.info("Preparing to run instrumented code")
        logger.debug(new String(instrumented))

        val evaluator = new WorksheetEvaluator(scriptUnit.scalaProject)
        evaluator.eval(fullName, instrumented)

      case Right(ex) =>
        // it may look funny to transform a `Right` into a `Left`. That's because the presentation compiler
        // does not use the convention that success is in `Right`. We follow it, like everyone else, but we need
        // to translate it here
        Left(InstrumentedError(ex))
    }
  }
}