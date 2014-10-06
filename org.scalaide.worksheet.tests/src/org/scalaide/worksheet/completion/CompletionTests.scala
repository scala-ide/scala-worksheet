package org.scalaide.worksheet.completion

import org.scalaide.core.testsetup.TestProjectSetup
import org.scalaide.core.testsetup.SDTTestUtils
import scala.reflect.internal.util.OffsetPosition
import org.scalaide.core.completion.CompletionProposal
import org.scalaide.core.completion.ScalaCompletions
import org.scalaide.worksheet.ScriptCompilationUnit
import org.eclipse.core.runtime.Path
import org.eclipse.core.resources.IFile
import scala.tools.nsc.interactive.Response
import org.scalaide.util.ScalaWordFinder
import org.junit.Assert
import org.junit.Test

object CompletionTests extends TestProjectSetup("completion", bundleName = "org.scalaide.worksheet.tests")

/**  Copy-pasted and adapted from sdt.core.tests. Needs some refactoring
 */
class CompletionTests {
  import CompletionTests._

  private def withCompletions(path2source: String)(body: (Int, OffsetPosition, List[CompletionProposal]) => Unit) {
    project // just kick in initialization

    val ifile = SDTTestUtils.workspace.getRoot.findMember(new Path(path2source)).asInstanceOf[IFile]
    val unit = ScriptCompilationUnit(ifile)

    // first, 'open' the file by telling the compiler to load it
    unit.withSourceFile { (src, compiler) =>
      val dummy = new Response[Unit]
      compiler.askReload(List(src), dummy)
      dummy.get

      val tree = new Response[compiler.Tree]
      compiler.askLoadedTyped(src, true, tree)
      tree.get

      val contents = unit.getContents
      // mind that the space in the marker is very important (the presentation compiler
      // seems to get lost when the position where completion is asked
      val positions = SDTTestUtils.positionsOf(contents, " /*!*/")
      val content = unit.getContents.mkString

      val completion = new ScalaCompletions
      for (i <- 0 until positions.size) {
        val pos = positions(i)

        val position = new OffsetPosition(src, pos)
        val wordRegion = ScalaWordFinder.findWord(content, position.point)

        //        val selection = mock(classOf[ISelectionProvider])

        /* FIXME:
         * I would really love to call `completion.computeCompletionProposals`, but for some unclear
         * reason that call is not working. Some debugging shows that the position is not right (off by one),
         * however, increasing the position makes the computed `wordRegion` wrong... hard to understand where
         * the bug is!
        val textViewer = mock(classOf[ITextViewer])
        when(textViewer.getSelectionProvider()).thenReturn(selection)
        val document = mock(classOf[IDocument])
        when(document.get()).thenReturn(content)
        when(textViewer.getDocument()).thenReturn(document)
        val monitor = mock(classOf[IProgressMonitor])
        val context = new ContentAssistInvocationContext(textViewer, position.offset.get)
        import collection.JavaConversions._
        val completions: List[ICompletionProposal] = completion.computeCompletionProposals(context, monitor).map(_.asInstanceOf[ICompletionProposal]).toList
        */

        body(i, position, completion.findCompletions(wordRegion)(pos + 1, unit)(src, compiler))
      }
    }
  }

  import Assert._

  /** @param withImportProposal take in account proposal for types not imported yet
   */
  private def runTest(path2source: String, withImportProposal: Boolean)(expectedCompletions: List[String]*) {

    withCompletions(path2source) { (i, position, compl) =>

      val completions = if (!withImportProposal) compl.filter(!_.needImport) else compl

      // remove parens as the compiler trees' printer has been slightly modified in 2.10
      // (and we need the test to pass for 2.9.0/-1 and 2.8.x as well).
      val completionsNoParens: List[String] = completions.map(c => normalizeCompletion(c.display)).sorted
      val expectedNoParens: List[String] = expectedCompletions(i).map(normalizeCompletion).sorted

      println("Found following completions @ position (%d,%d):".format(position.line, position.column))
      completionsNoParens.foreach(e => println("\t" + e))
      println()

      println("Expected completions:")
      expectedNoParens.foreach(e => println("\t" + e))
      println()

      assertTrue("Found %d completions @ position (%d,%d), Expected %d"
        .format(completionsNoParens.size, position.line, position.column, expectedNoParens.size),
        completionsNoParens.size == expectedNoParens.size) // <-- checked condition

      completionsNoParens.zip(expectedNoParens).foreach {
        case (found, expected) =>
          assertEquals("Found `%s`, expected `%s`".format(found, expected), expected, found)
      }
    }
  }

  /** Transform the given completion proposal into a string that is (hopefully)
   *  compiler-version independent.
   *
   *  Transformations are:
   *    - remove parenthesis
   *    - java.lang.String => String
   */
  private def normalizeCompletion(str: String): String = {
    str.replace("(", "").replace(")", "").replace("java.lang.String", "String")
  }

  @Test
  def basicCompletions() {
    val oraclePos73 = List("toString(): String")
    val oraclePos116 = List("forallp: Char => Boolean: Boolean")
    val oraclePos147 = List("forallp: Char => Boolean: Boolean")

    runTest("completion/src/org/example/ScalaClass.scala", false)(oraclePos73, oraclePos116, oraclePos147)
  }
}