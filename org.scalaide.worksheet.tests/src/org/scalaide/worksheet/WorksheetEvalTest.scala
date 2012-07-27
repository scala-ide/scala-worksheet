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

class WorksheetEvalTest {
  import SDTTestUtils._
  
  @Test
  def testEvaluation() {
    val initialContents = """
object MyMain {
  val xs = List(1, 2, 3)

  xs.map(_ * 2)
}
      
"""
    
    val Seq(prj) = createProjects("eval-test")
    
    val bytes = new ByteArrayInputStream(initialContents.getBytes)
    
    val iFile = workspace.getRoot.getFile(new Path("eval-test/mytest.sc"))
    iFile.create(bytes, IResource.NONE, null)
    
    val mockedDoc = mock(classOf[IDocument])
    when(mockedDoc.get).thenReturn(initialContents)
    
    val eval = new WorksheetEvaluator(prj, mockedDoc)
    val scriptUnit = ScriptCompilationUnit(iFile)
    prj.withPresentationCompiler { compiler =>
//        val source = scriptUnit.sourceFile(mockedDoc.get)
        val source = new BatchSourceFile(EclipseResource(iFile), mockedDoc.get)
        compiler.withResponse[Unit] { compiler.askReload(List(source), _) } // just make sure it's loaded
        compiler.withResponse[(String, Array[Char])] { compiler.askInstrumented(source, -1, _) }.get
      }() match {
        case Left((fullName, instrumented)) =>
          println("Preparing to run instrumented code")
          println(instrumented.mkString)

          val evaluator = new WorksheetEvaluator(scriptUnit.scalaProject, mockedDoc)
          val result = evaluator.eval(fullName, instrumented)
          
          println(result)

        case Right(ex) =>
          println("Error during `askInstrumented`", ex)
      }
  }
}