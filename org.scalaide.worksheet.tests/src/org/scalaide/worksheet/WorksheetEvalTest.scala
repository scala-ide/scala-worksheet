package org.scalaide.worksheet

import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.Path
import org.eclipse.jface.text.IDocument
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.when
import org.scalaide.worksheet.handlers.EvalScript
import java.io.ByteArrayInputStream
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.testsetup.SDTTestUtils.createProjects
import scala.tools.eclipse.testsetup.SDTTestUtils.deleteProjects
import scala.tools.eclipse.testsetup.SDTTestUtils.workspace
import scala.tools.nsc.scratchpad.Mixer
import scala.tools.nsc.scratchpad.SourceInserter
import scala.tools.eclipse.testsetup.SDTTestUtils

class WorksheetEvalTest {
  import SDTTestUtils._

  @Ignore
  @Test
  def simple_evaluation_succeeds() {
    val initial = """
object Main {
  val xs = List(1, 2, 3)
  xs.max                
  val ys = Seq(1, 2, 3, 3,4 )
}
      
"""

    val expected = """
object Main {
  val xs = List(1, 2, 3)                          //> xs : List[Int] = List(1, 2, 3)
  xs.max                                          //> res0: Int = 3
  val ys = Seq(1, 2, 3, 3,4 )                     //> ys : Seq[Int] = List(1, 2, 3, 3, 4)
}
"""
    //runEvalTest("eval-test/test1.sc", initial, expected)
  }

  @Ignore
  @Test
  def multiline_output() {
    val initial = """
object Main {
  val xs = List(1, 2, 3)

  xs foreach println                             
}
"""

    val expected = """
object Main {
  val xs = List(1, 2, 3)                          //> xs : List[Int] = List(1, 2, 3)

  xs foreach println                              //> 1
                                                  //| 2
                                                  //| 3
}
"""

   // runEvalTest("eval-test/test2.sc", initial, expected)
  }

  @Ignore
  @Test
  def escapes_in_input() {
    val initial = """
object Main {
  val xs = List(1, 2, 3)

  println("\none\ntwo\nthree")                     
}
"""

    val expected = """
object Main {
  val xs = List(1, 2, 3)                          //> xs : List[Int] = List(1, 2, 3)

  println("\none\ntwo\nthree")                    //> 
                                                  //| one
                                                  //| two
                                                  //| three
}
"""

   // runEvalTest("eval-test/test3.sc", initial, expected)
  }

  private var project: ScalaProject = _

  @Before
  def createProject() {
    val Seq(prj) = createProjects("eval-test")
    project = prj

    project.sourceOutputFolders.map {
      case (_, outp: IFolder) => outp.create(true, true, null)
    }
  }

  @After
  def deleteProject() {
    deleteProjects(project)
  }

//  def runEvalTest(filename: String, contents: String, expected: String) {
//    val bytes = new ByteArrayInputStream(contents.getBytes)
//
//    val iFile = workspace.getRoot.getFile(new Path(filename))
//    iFile.create(bytes, IResource.NONE, null)
//
//    val mockedDoc = mock(classOf[IDocument])
//    when(mockedDoc.get).thenReturn(contents)
//
//    val eval = new EvalScript
//    val scriptUnit = ScriptCompilationUnit(iFile)
//    val res = eval.evalDocument(scriptUnit, mockedDoc)
//
//    Assert.assertTrue("Should succeed: " + res, res.isRight)
//
//    val mixer = new Mixer
//    val stripped = SourceInserter.stripRight(contents.toCharArray())
//    val Right(result) = res
//    val mixed = mixer.mix(stripped, result.toCharArray)
//
//    Assert.assertEquals("correct output", expected.trim, mixed.mkString.trim)
//  }
}