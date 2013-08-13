package org.scalaide.worksheet.runtime

import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.testsetup.SDTTestUtils.createProjects
import scala.tools.eclipse.testsetup.SDTTestUtils.deleteProjects
import org.eclipse.core.resources.IFolder
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.properties.WorksheetPreferences
import org.scalaide.worksheet.testutil.EvalTester.wipeResults
import org.junit.Ignore

object ClearResultsTest {
  @BeforeClass
  def createProject() {
    val Seq(prj) = createProjects("clear-results-test")
    project = prj

    project.sourceOutputFolders.map {
      case (_, outp: IFolder) => outp.create(true, true, null)
    }
  }

  private var project: ScalaProject = _

  @AfterClass
  def deleteProject() {
    deleteProjects(project)
  }

}

class ClearResultsTest {

  @Test
  def simple_cleaning_succeeds() {
    val initial =
      """|object Main {
         |  val xs = List(1, 2, 3)                          //> xs  : List[Int] = List(1, 2, 3)
         |  xs.max                                          //> res0: Int = 3
         |  val ys = Seq(1, 2, 3, 3,4 )                     //> ys  : Seq[Int] = List(1, 2, 3, 3, 4)
         |}""".stripMargin

    val expected =
      """|object Main {
         |  val xs = List(1, 2, 3)
         |  xs.max                
         |  val ys = Seq(1, 2, 3, 3,4 )
         |}""".stripMargin

    runTest(initial, expected)
  }

  @Test
  def multiline_output() {
    val initial =
      """|object Main {
         |  val xs = List(1, 2, 3)                          //> xs  : List[Int] = List(1, 2, 3)
         |
         |  xs foreach println                              //> 1
         |                                                  //| 2
         |                                                  //| 3
         |}""".stripMargin

    val expected =
      """|object Main {
         |  val xs = List(1, 2, 3)
         |
         |  xs foreach println                             
         |}""".stripMargin

    runTest(initial, expected)
  }

  /** Run the eval test using the initial contents and check against the given expected output.
   */
  def runTest(contents: String, expected: String) {
    val res = wipeResults(contents)

    Assert.assertEquals("correct output", expected.trim, res.trim)
  }
}
