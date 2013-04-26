package org.scalaide.worksheet.runtime

import java.io.File
import java.io.{FileOutputStream, OutputStreamWriter}
import scala.tools.eclipse.ScalaProject
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import java.nio.charset.Charset

private[runtime] object Configuration {
  private val RootFolder = new Path(".worksheet")
  private val SrcFolder = RootFolder.append("src")
  private val BinFolder = RootFolder.append("bin")

  def apply(project: ScalaProject): Configuration =
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
    val folder = createFolder(absolutePath)
    folder
  }

  private def createFolder(absolutePath: IPath): File = create(absolutePath) { folder =>
    folder.mkdirs() // FIXME: This is not really safe
    assert(folder.isDirectory())
  }

  private def createFile(absolutePath: IPath): File = create(absolutePath) { file =>
    file.createNewFile() // FIXME: This is not really safe
    assert(file.isFile())
  }

  private def create(absolutePath: IPath)(f: File => Unit): File = {
    val file = new File(absolutePath.toOSString())
    f(file)
    file
  }

  def clearSrcFolder(): Unit = clear(srcFolder)
  def clearBinFolder(): Unit = clear(binFolder)

  private def clear(folder: File): Unit = {
    if (folder.isDirectory()) {
      for (resource <- folder.listFiles())
        resource.delete() // FIXME: This is not really safe
    }
  }

  def touchSource(name: String, content: Array[Char], encoding: Charset): File = {
    val absolutePath = new Path(srcFolder.getAbsolutePath()).append(name)
    val source = createFile(absolutePath)
    assert(source.isFile())

    val writer = new OutputStreamWriter(new FileOutputStream(source), encoding.name)
    try writer.write(content)
    finally writer.close() // FIXME: This is not really safe

    source
  }
}