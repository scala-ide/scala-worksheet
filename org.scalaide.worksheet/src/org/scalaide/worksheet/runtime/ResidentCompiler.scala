package org.scalaide.worksheet.runtime

import java.io.File

import scala.reflect.internal.util.Position
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter

import org.scalaide.core.IScalaProject
import org.scalaide.logging.HasLogger
import org.scalaide.worksheet.WorksheetPlugin
import scala.tools.nsc.settings.SpecificScalaVersion
import scala.tools.nsc.settings.ScalaVersion

/**
 * @deprecated replaced by [[org.scalaide.core.internal.builder.zinc.ResidentCompiler]]
 */
object ResidentCompiler extends HasLogger {
  def apply(scalaProject: IScalaProject, worksheetConfig: Configuration): ResidentCompiler = {
    val scalaClassPath = scalaProject.scalaClasspath
    def version: String =
      scalaProject.effectiveScalaInstallation.version match {
        case SpecificScalaVersion(major, minor, _, _) => s"$major.$minor"
        case _ =>
          val SpecificScalaVersion(major, minor, _, _) = ScalaVersion.current
          s"$major.$minor"
      }

    // scalacArguments returns all project settings (but not user classpath, nor output directory)
    val args = scalaProject.scalacArguments ++ Seq(
      "-classpath", (WorksheetPlugin.worksheetLibraries.get(version).map(_.toOSString()).toSeq ++ scalaClassPath.userCp.map(_.toFile.getAbsolutePath)).mkString(File.pathSeparator),
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