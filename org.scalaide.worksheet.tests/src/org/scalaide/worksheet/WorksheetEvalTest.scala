package org.scalaide.worksheet

import org.junit.Test
import scala.tools.eclipse.testsetup.SDTTestUtils
import org.mockito.Mockito._
import org.eclipse.jface.text.IDocument
import org.scalaide.worksheet.eval.WorksheetEvaluator
import scala.reflect.internal.util.ScriptSourceFile
import org.eclipse.core.runtime.Path
import java.io.ByteArrayInputStream
import org.eclipse.core.resources.IResource
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.eclipse.util.EclipseResource
import org.scalaide.worksheet.handlers.EvalScript
import org.junit.Assert

class WorksheetEvalTest {
  import SDTTestUtils._
  
  @Test
  @Ignore("Enable when the evaluation engine in the Scala compiler is ready")
  def testEvaluation() {
    val initialContents = """
object MyMain {
  val xs = List(1, 2, 3)

  xs.map(_ * 2)
}
      
"""
      
   val evaluated = """
object MyMain {
  val xs = List(1, 2, 3)  // TO BE UPDATED

  xs.map(_ * 2)
}
      
"""    
    val Seq(prj) = createProjects("eval-test")
    
    val bytes = new ByteArrayInputStream(initialContents.getBytes)
    
    val iFile = workspace.getRoot.getFile(new Path("eval-test/mytest.sc"))
    iFile.create(bytes, IResource.NONE, null)
    
    val mockedDoc = mock(classOf[IDocument])
    when(mockedDoc.get).thenReturn(initialContents)
    
    val eval = new EvalScript
    val scriptUnit = ScriptCompilationUnit(iFile)
    val res = eval.evalDocument(scriptUnit, mockedDoc)
    
    Assert.assertTrue("Should succeed: " + res, res.isRight)
    Assert.assertEquals("correct output: " + res, res.right.get)
  }
}