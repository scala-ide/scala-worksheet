package org.scalaide.worksheet.runtime

import java.io.File
import scala.tools.nsc.interactive.ProgramInstrumenter
import scala.tools.eclipse.logging.HasLogger
import org.scalaide.worksheet.ScriptCompilationUnit
import org.scalaide.worksheet.text.SourceInserter
import java.nio.charset.Charset

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
    unit.scalaProject.withPresentationCompiler { compiler =>
      val instrumenter = new ProgramInstrumenter(compiler)
      val source = unit.batchSourceFile(SourceInserter.stripRight(unit.getContents))
      compiler.withResponse[(String, Array[Char])] { instrumenter.askInstrumented(source, -1, _) }.get
    }() match {
      case Left((fullName, program)) =>
        if (fullName.isEmpty) Left(MissingTopLevelObjectDeclaration(unit))
        else Right(InstrumentationResult(TopLevelObjectDecl(fullName), program))

      case Right(ex) =>
        Left(ProgramInstrumentationFailed(unit, ex))
    }
  }

  /** Write instrumented source file to disk. */
  private def writeInstrumented(decl: TopLevelObjectDecl, content: Array[Char]): File = {
    val sourceName = decl.fullName + ".scala"
    val sourceFile = config.touchSource(sourceName, content, config.vmArgs.fileEncoding)
    sourceFile
  }
}