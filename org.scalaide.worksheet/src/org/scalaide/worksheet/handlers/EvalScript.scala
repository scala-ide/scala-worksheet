package org.scalaide.worksheet.handlers

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.ui.handlers.HandlerUtil
import org.scalaide.worksheet.ScriptCompilationUnit
import org.eclipse.ui.texteditor.ITextEditor
import scala.tools.eclipse.logging.HasLogger
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.scratchpad.SourceInserter
import scala.tools.nsc.scratchpad.Executor
import scala.tools.nsc.io.{ AbstractFile, VirtualDirectory }
import scala.tools.nsc.reporters.ConsoleReporter
import java.io.FileWriter
import scala.tools.eclipse.buildmanager.sbtintegration.BasicConfiguration
import scala.tools.eclipse.buildmanager.sbtintegration.ScalaCompilerConf
import org.scalaide.worksheet.eval.WorksheetEvaluator

class EvalScript extends AbstractHandler with HasLogger {

  override def execute(event: ExecutionEvent): AnyRef = {
    for {
      editor <- Option(HandlerUtil.getActiveEditor(event).asInstanceOf[ITextEditor])
      editorInput <- Option(HandlerUtil.getActiveEditorInput(event))
      scriptUnit <- ScriptCompilationUnit.fromEditorInput(editorInput)
    } {
      val doc = editor.getDocumentProvider.getDocument(editorInput)
      scriptUnit.scalaProject.withPresentationCompiler { compiler =>
        val source = scriptUnit.sourceFile(doc.get)
        compiler.withResponse[Unit] { compiler.askReload(List(source), _) } // just make sure it's loaded
        compiler.withResponse[(String, Array[Char])] { compiler.askInstrumented(source, -1, _) }.get
      }() match {
        case Left((fullName, instrumented)) =>
          logger.info("Preparing to run instrumented code")
          println(new String(instrumented))

          val evaluator = new WorksheetEvaluator(scriptUnit.scalaProject, doc)
          val result = evaluator.eval(fullName, instrumented)

          if (result.length > 0)
            doc.replace(0, doc.getLength, new String(result))

        case Right(ex) =>
          eclipseLog.error("Error during `askInstrumented`", ex)
      }
    }

    null
  }

}