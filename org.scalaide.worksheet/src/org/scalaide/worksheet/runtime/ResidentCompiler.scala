package org.scalaide.worksheet.runtime

import java.io.File
import org.scalaide.core.IScalaPlugin
import org.scalaide.core.api.ScalaProject
import org.scalaide.logging.HasLogger
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter
import scala.reflect.internal.util.Position
import org.scalaide.worksheet.WorksheetPlugin

object ResidentCompiler extends HasLogger {
  def apply(scalaProject: ScalaProject, worksheetConfig: Configuration): ResidentCompiler = {
    val scalaClassPath = scalaProject.scalaClasspath

    // scalacArguments returns all project settings (but not user classpath, nor output directory)
    val args = scalaProject.scalacArguments ++ Seq(
        "-classpath", (WorksheetPlugin.worksheetLibrary.map(_.toOSString()).toSeq ++ scalaClassPath.userCp.map(_.toFile.getAbsolutePath)).mkString(File.pathSeparator),
        "-d", worksheetConfig.binFolder.getAbsolutePath())

    logger.debug("Compilation arguments: " + args.mkString("\n"))
    new ResidentCompiler(args)
  }

  sealed abstract class CompilationResult
  case object CompilationSuccess extends CompilationResult
  case class CompilationFailed(errors: Iterable[CompilationError]) extends CompilationResult

  case class CompilationError(msg: String, pos: Position)
}

class ResidentCompiler private (arguments: Seq[String]) extends HasLogger {
  import ResidentCompiler._

  private val reporter = new StoreReporter()
  private val settings = new Settings(println(_))
  private val compiler = new scala.tools.nsc.Global(settings, reporter)

  def compile(source: File): CompilationResult = this.synchronized {
    runCompilation(source)

    if (reporter.hasErrors) {
      val errors = reporter.infos map { error => CompilationError(error.msg, error.pos) }
      reporter.reset()
      CompilationFailed(errors)
    } else CompilationSuccess
  }

  private def runCompilation(source: File): Unit = {
    val sources = source.getAbsolutePath()
    logger.debug("compiling " + sources)

    val command = new CompilerCommand(sources :: arguments.toList, settings)
    val run = new compiler.Run()
    logger.info("compiling: " + command.files)
    run compile command.files
  }
}