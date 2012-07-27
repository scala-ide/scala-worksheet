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

class EvalScript extends AbstractHandler with HasLogger {

  override def execute(event: ExecutionEvent): AnyRef = {
    for {
      editor <- Option(HandlerUtil.getActiveEditor(event).asInstanceOf[ITextEditor])
      editorInput <- Option(HandlerUtil.getActiveEditorInput(event))
      scriptUnit <- ScriptCompilationUnit.fromEditorInput(editorInput)
    } {
      val doc = editor.getDocumentProvider.getDocument(editorInput)
      scriptUnit.scalaProject.withPresentationCompiler { compiler =>
        val source = scriptUnit.batchSourceFile(SourceInserter.stripRight(doc.get.toCharArray))
        compiler.withResponse[Unit] { compiler.askReload(List(source), _) } // just make sure it's loaded
        compiler.withResponse[(String, Array[Char])] { compiler.askInstrumented(source, -1, _) }.get
      }() match {
        case Left((fullName, instrumented)) =>
          logger.info("Preparing to run instrumented code")
          logger.debug(new String(instrumented))

          val evaluator = new WorksheetEvaluator(scriptUnit.scalaProject, doc)
          evaluator.eval(fullName, instrumented) match {
            case Left(reporter) =>
              MessageDialog.openError(ScalaPlugin.getShell,
                "Error during evaluation",
                "Compilation errors during the evaluation of instrumented code\n(please open a bug report):\n\n" + reporter.infos.map(_.msg).mkString("\n"))

            case Right(result) =>
              logger.debug(result)
              if (result.length > 0)
                doc.replace(0, doc.getLength, result)
          }

        case Right(ex) =>
          eclipseLog.error("Error during `askInstrumented`", ex)
      }
    }

    null
  }

}