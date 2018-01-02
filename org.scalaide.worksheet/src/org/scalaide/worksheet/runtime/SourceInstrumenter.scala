package org.scalaide.worksheet.runtime

import java.io.File

import scala.Left
import scala.Right
import scala.tools.nsc.interactive.ProgramInstrumenter

import org.scalaide.core.compiler.IScalaPresentationCompiler
import org.scalaide.logging.HasLogger
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.text.SourceInserter

case class TopLevelObjectDecl(fullName: String)

class SourceInstrumenter(config: Configuration) extends HasLogger {
  /** Encapsulate the result of a call to `compiler.askInstrumented`.
   *  @param fullName the fully qualified name of the first top-level object definition in the instrumented program.
   *  @param program the text of the instrumented program which will be ran
   */
  case class InstrumentationResult(decl: TopLevelObjectDecl, program: Array[Char])

  def instrument(unit: ScriptCompilationUnit): Either[Throwable, (TopLevelObjectDecl, File)] = {
    instrumentProgram(unit).right map {
      case InstrumentationResult(decl, program) =>
        (decl, writeInstrumented(decl, program))
    }
  }

  private def instrumentProgram(unit: ScriptCompilationUnit): Either[Throwable, InstrumentationResult] = {
    unit.scalaProject.presentationCompiler { compiler =>
      val instrumenter = new ProgramInstrumenter(compiler)
      val source = unit.batchSourceFile(SourceInserter.stripRight(unit.getContents))
      IScalaPresentationCompiler.withResponse[(String, Array[Char])] { instrumenter.askInstrumented(source, -1, _) }.get
    } match {
      case Some(Left((fullName, program))) =>
        if (fullName.isEmpty) Left(MissingTopLevelObjectDeclaration(unit))
        else Right(InstrumentationResult(TopLevelObjectDecl(fullName), program))

      case Some(Right(ex)) =>
        Left(ProgramInstrumentationFailed(unit, ex))
      case None => Left(ProgramInstrumentationFailed(unit, new UnsupportedOperationException("Presentation compiler exception on unsupported operation")))
    }
  }

  /** Write instrumented source file to disk. */
  private def writeInstrumented(decl: TopLevelObjectDecl, content: Array[Char]): File = {
    val sourceName = decl.fullName + ".scala"
    logger.info(s"writing $sourceName with content ${content.take(20).mkString}")
    val sourceFile = config.touchSource(sourceName, content, config.vmArgs.fileEncoding)
    sourceFile
  }
}