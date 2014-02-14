package org.scalaide.worksheet.runtime

import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import org.scalaide.logging.HasLogger
import org.eclipse.core.resources.IProject
import scala.Array.canBuildFrom

class VmArguments(project: IProject, rawArgs: String) extends HasLogger {
  private def projectEncoding: Charset = asCharset(project.getDefaultCharset())

  private def asCharset(encoding: String): Charset = {
    try Charset.forName(encoding)
    catch {
      case _: IllegalCharsetNameException | _: IllegalArgumentException | _: UnsupportedCharsetException =>
        val defaultEncoding: Charset = Charset.forName("UTF-8")
        eclipseLog.error("Unrecognized project's encoding '%s' in project '%s'. Using '%s'.".format(projectEncoding, project.getName, defaultEncoding))
        defaultEncoding
    }
  }

  // obtain and assign VM arguments, split by whitespace
  // empty string arguments are eliminated, since the jvm will regard one as a main class argument.
  private val cleanedArgs = rawArgs.split("""\s""").filterNot(_.isEmpty)

  private val shouldUseProjectEncoding: Boolean =
    !cleanedArgs.exists(_.matches("-Dfile.encoding=\\S"))

  def args: Array[String] = {
    if (shouldUseProjectEncoding) cleanedArgs :+ ("-Dfile.encoding=" + projectEncoding)
    else cleanedArgs
  }

  def fileEncoding: Charset = {
    if(shouldUseProjectEncoding) projectEncoding
    else {
      val encodingVmArg = cleanedArgs.find(_.matches("-Dfile.encoding=\\S")).get
      val encodingValue = encodingVmArg.stripPrefix("-Dfile.encoding=")
      asCharset(encodingValue)
    }
  }
}