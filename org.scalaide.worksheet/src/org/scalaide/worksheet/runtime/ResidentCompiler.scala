package org.scalaide.worksheet.runtime

import java.io.File

import scala.tools.nsc.util.Position
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.logging.HasLogger
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter

object ResidentCompiler extends HasLogger {
  def apply(scalaProject: ScalaProject, worksheetConfig: Configuration): ResidentCompiler = {
    val scalaClassPath = scalaProject.scalaClasspath
    val bootClasspathArgs: String = scalaClassPath.scalaLib.get.toFile.getAbsolutePath
    val jrePath = scalaClassPath.jdkPaths.map(_.toFile)
    val additionalCpEntries = scalaProject.outputFolderLocations :+ ScalaPlugin.plugin.compilerClasses.get

    // FIXME: We are currently ignoring the project's settings, which is bad. (What if the user wants to enable 
    //        continuations in the worksheet!)
    val args = List("-bootclasspath", bootClasspathArgs,
      "-javabootclasspath", jrePath.map(_.getAbsolutePath).mkString(File.pathSeparator),
      "-classpath", additionalCpEntries.map(_.toFile.getAbsolutePath).mkString(File.pathSeparator),
      "-d", worksheetConfig.binFolder.getAbsolutePath())

    logger.debug("Compilation arguments: " + args)
    new ResidentCompiler(args)
  }
  

  sealed abstract class CompilationResult
  case object CompilationSuccess extends CompilationResult
  sealed case class CompilationFailed(errors: Iterable[CompilationError]) extends CompilationResult

  case class CompilationError(msg: String, pos: Position)
}

class ResidentCompiler private (arguments: List[String]) extends HasLogger {
  import ResidentCompiler._

  private val reporter = new StoreReporter()
  private val settings = new Settings(println(_)) // FIXME: This should really use the `scalaProject` settings (otherwise, how to enable continuations?)
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
    val sourcePath = source.getAbsolutePath()
    logger.debug("compiling " + sourcePath)

    val command = new CompilerCommand(sourcePath :: arguments, settings)
    val run = new compiler.Run()
    logger.info("compiling: " + command.files)
    run compile command.files
  }
}