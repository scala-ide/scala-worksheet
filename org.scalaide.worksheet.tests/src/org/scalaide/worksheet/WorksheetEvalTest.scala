package org.scalaide.worksheet

import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.testsetup.SDTTestUtils.createProjects
import scala.tools.eclipse.testsetup.SDTTestUtils.deleteProjects
import org.eclipse.core.resources.IFolder
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import scala.tools.eclipse.testsetup.SDTTestUtils
import org.junit.Assert
import org.scalaide.worksheet.testutil.EvalTester
import org.junit.BeforeClass
import org.junit.AfterClass
import org.scalaide.worksheet.properties.WorksheetPreferences

object WorksheetEvalTest {
  @BeforeClass
  def createProject() {
    val Seq(prj) = createProjects("eval-test")
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

class WorksheetEvalTest {
  import SDTTestUtils._

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
  val xs = List(1, 2, 3)                          //> xs  : List[Int] = List(1, 2, 3)
  xs.max                                          //> res0: Int = 3
  val ys = Seq(1, 2, 3, 3,4 )                     //> ys  : Seq[Int] = List(1, 2, 3, 3, 4)
}
"""
    runTest("eval-test/test1.sc", initial, expected)
  }

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
  val xs = List(1, 2, 3)                          //> xs  : List[Int] = List(1, 2, 3)

  xs foreach println                              //> 1
                                                  //| 2
                                                  //| 3
}
"""

    runTest("eval-test/test2.sc", initial, expected)
  }

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
  val xs = List(1, 2, 3)                          //> xs  : List[Int] = List(1, 2, 3)

  println("\none\ntwo\nthree")                    //> 
                                                  //| one
                                                  //| two
                                                  //| three
}
"""

    runTest("eval-test/test3.sc", initial, expected)
  }

  @Test
  def longOutput_is_cut() {
    val initial = """
object testeval {
  var x = 0

  while (x < 100) {
    x += 1
    println(x)
  }
}
"""

    val expected = """
object testeval {
  var x = 0                                       //> x  : Int = 0

  while (x < 100) {
    x += 1
    println(x)
  }                                               //> 1
                                                  //| 2
                                                  //| 3
                                                  //| 4
                                                  //| 5
                                                  //| 6
                                                  //| 7
                                                  //| 8
                                                  //| 9
                                                  //| 10
                                                  //| Output exceeds cutoff limit.
}"""

    withCutOffValue(50) { runTest("eval-test/test4.sc", initial, expected) }
  }

  @Test
  def inifiteLoop_is_cut() {
    val initial = """
object testeval {
  var x = 0

  while (true) {
    x += 1
    println(x)
  }
}
"""

    val expected = """
object testeval {
  var x = 0                                       //> x  : Int = 0

  while (true) {
    x += 1
    println(x)
  }                                               //> 1
                                                  //| 2
                                                  //| 3
                                                  //| 4
                                                  //| 5
                                                  //| 6
                                                  //| 7
                                                  //| 8
                                                  //| 9
                                                  //| 10
                                                  //| Output exceeds cutoff limit.
}"""
    import EvalTester._
    withCutOffValue(50) {
      val res = runEvalSync("eval-test/test5.sc", initial, 5000)
      val lastChar = res.init.trim.last // last character is }, we go one before that
      Assert.assertTrue("Last character is spinner: " + lastChar, Set('/', '|', '-', '\\').contains(lastChar))
      Assert.assertEquals("correct output", expected.init.trim, res.init.trim.init.trim) // we cut the last character
    }
  }

  @Test
  def parens_eval() {
    val initial = """
object Main {
  (1 + 2)
}
      
"""

    val expected = """
object Main {
  (1 + 2)                                         //> res0: Int(3) = 3
}
"""
    runTest("eval-test/parens.sc", initial, expected)
  }

  @Test
  def for_yield_eval() {
    val initial = """
object Main {
  for (x <- 1 to 3) yield x*x
}
      
"""
    val expected = """
object Main {
  for (x <- 1 to 3) yield x*x                     //> res0: scala.collection.immutable.IndexedSeq[Int] = Vector(1, 4, 9)
}
"""
    runTest("eval-test/foryield.sc", initial, expected)
  }

  @Test
  def symbolic_names() {
    val initial = """
object Main {
  val ?? = 10
}
      
"""
    val expected = """
object Main {
  val ?? = 10                                     //> ??  : Int = 10
}
"""
    runTest("eval-test/symbolic.sc", initial, expected)
  }
  
  @Test
  def cutOff_is_perEvaluation_t97() {
    val initial = """
object  myws { 
   val even = for(i <- 1 until 1000) yield 2*i
   val odd  = for(i <- 1 until 1000) yield (2*i+1)
}
"""

    val expected = """
object  myws {
   val even = for(i <- 1 until 1000) yield 2*i    //> even  : scala.collection.immutable.IndexedSeq[Int] = Vector(2, 4, 6, 8, 10, 1
                                                  //| 2, 14, 16, 18, 2
                                                  //| Output exceeds cutoff limit.
   val odd  = for(i <- 1 until 1000) yield (2*i+1)//> odd  : scala.collection.immutable.IndexedSeq[Int] = Vector(3, 5, 7, 9, 11, 1
                                                  //| 3, 15, 17, 19, 
                                                  //| Output exceeds cutoff limit.
}"""

    withCutOffValue(100) { runTest("eval-test/test97.sc", initial, expected) }
  }
  
  /** Temporarily set the cut off value to `v`. */
  private def withCutOffValue(v: Int)(block: => Unit) {
    val prefs = WorksheetPlugin.plugin.getPreferenceStore()
    val oldCutoff = prefs.getInt(WorksheetPreferences.P_CUTOFF_VALUE)
    prefs.setValue(WorksheetPreferences.P_CUTOFF_VALUE, v)
    try {
      block
    } finally prefs.setValue(WorksheetPreferences.P_CUTOFF_VALUE, oldCutoff)
  }

  /** Run the eval test using the initial contents and check against the given expected output.
   *
   *  The `filename` must not exist, and it must be an absolute path, starting at the workspace root.
   *  The test blocks for at most `timeout` millis, but will finish as soon as the evaluator signals
   *  termination by calling 'endUpdate' on the `DocumentHolder` interface.
   */
  def runTest(filename: String, contents: String, expected: String, timeout: Int = 30000) {
    import EvalTester._

    val res = runEvalSync(filename, contents, timeout)

    Assert.assertEquals("correct output", expected.trim, res.trim)
  }
}
