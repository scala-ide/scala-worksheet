package org.scalaide.worksheet

import scala.tools.nsc.settings.SpecificScalaVersion

import org.junit.Assert
import org.scalaide.core.internal.project.ScalaInstallation
import org.scalaide.core.internal.project.ScalaInstallationChoice
import org.scalaide.core.internal.project.ScalaProject
import org.scalaide.worksheet.testutil.EvalTester

package object cross {
  private object Monitor
  private def setScalaVersion(project: ScalaProject, major: Int, minor: Int): Unit = {
    project.effectiveScalaInstallation.version match {
      case SpecificScalaVersion(2, `minor`, _, _) =>
      case _ =>
        val requiredInstallation = ScalaInstallation.availableInstallations.map { installation =>
          installation -> installation.version
        }.collectFirst {
          case (installation, SpecificScalaVersion(2, `minor`, _, _)) => installation
        }.get
        project.setDesiredInstallation(ScalaInstallationChoice(requiredInstallation.version))
    }
    Assert.assertTrue(project.effectiveScalaInstallation.version match {
      case SpecificScalaVersion(2, `minor`, _, _) => true
      case _ => false
    })
  }

  def evaluate(project: ScalaProject, major: Int, minor: Int): Unit = Monitor.synchronized {
    setScalaVersion(project, major, minor)
    val initial = """
object o {
  val a = 3
}
"""

    val expected = """
object o {
  val a = 3                                       //> a  : Int = 3
}
"""
    runTest(s"${project.underlying.getName}/test_${major}_$minor.sc", initial, expected)
  }

  private def runTest(filename: String, contents: String, expected: String, timeout: Int = 60000) {
    val res = EvalTester.runEvalSync(filename, contents, timeout)

    Assert.assertEquals("correct output", expected.trim, res.trim)
  }
}