package org.scalaide.worksheet.runtime

import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.Charset

import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.scalaide.core.IScalaProject
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.properties.WorksheetPreferences

private[runtime] object Configuration {
  private val RootFolder = new Path(".worksheet")
  private val SrcFolder = RootFolder.append("src")
  private val BinFolder = RootFolder.append("bin")

  def apply(project: IScalaProject): Configuration =
    new Configuration(project.underlying)
}

final private[runtime] class Configuration private (project: IProject) {
  import Configuration._

  @inline private def location = project.getLocation

  lazy val rootFolder: File = obtainFolder(RootFolder)
  lazy val srcFolder: File = {
    rootFolder
    obtainFolder(SrcFolder)
  }
  lazy val binFolder: File = {
    rootFolder
    obtainFolder(BinFolder)
  }

  private def obtainFolder(relativePath: IPath): File = {
    val absolutePath = location.append(relativePath)
    val folder = createFolder(absolutePath.toFile)
    folder
  }

  private def createFolder(absolutePath: File): File = {
    import java.nio.file.Files
    Files.createDirectories(absolutePath.toPath).toFile
  }

  private def autoClose[A <: AutoCloseable, B](resource: A)(code: A => B): B = {
    try
      code(resource)
    finally
      resource.close()
  }

  def touchSource(name: String, content: Array[Char], encoding: Charset): File = {
    import java.nio.file.Files
    val source = srcFolder.toPath.resolve(name)
    autoClose {
      new OutputStreamWriter(Files.newOutputStream(source))
    } { out =>
      out.write(content)
      source.toFile
    }
  }

  def vmArgs: VmArguments =
    new VmArguments(project, WorksheetPlugin.prefStore.getString(WorksheetPreferences.P_VM_ARGS))
}