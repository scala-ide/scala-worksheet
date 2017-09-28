package org.scalaide.worksheet

import scala.tools.nsc.settings.SpecificScalaVersion

import org.junit.Assert
import org.scalaide.core.internal.project.ScalaInstallation
import org.scalaide.core.internal.project.ScalaInstallationChoice
import org.scalaide.core.internal.project.ScalaProject
import org.scalaide.worksheet.testutil.EvalTester

package object cross {
  private object Monitor
  def evaluate(project: ScalaProject, major: Int, minor: Int, rev: Int): Unit = Monitor.synchronized {
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