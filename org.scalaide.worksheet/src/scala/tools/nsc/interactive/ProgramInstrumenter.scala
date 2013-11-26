package scala.tools.nsc.interactive

import scala.reflect.internal.util.SourceFile
import scala.tools.eclipse.ScalaPresentationCompiler
import scala.tools.eclipse.logging.HasLogger

final class ProgramInstrumenter(compiler: ScalaPresentationCompiler) extends HasLogger { self =>
  private object Instrumenter extends ScratchPadMaker2 {
    override protected val global: Global = self.compiler

    def askInstrumentation(source: SourceFile, line: Int, response: Response[(String, Array[Char])]): Unit =
      try {
        global.interruptsEnabled = false
        global.respond(response) {
          instrumentation(source, line)
        }
      } finally {
        global.interruptsEnabled = true
      }

    /** Compute an instrumented version of a sourcefile.
     *  @param source  The given sourcefile.
     *  @param line    The line up to which results should be printed, -1 = whole document.
     *  @return        A pair consisting of
     *                  - the fully qualified name of the first top-level object definition in the file.
     *                    or "" if there are no object definitions.
     *                  - the text of the instrumented program which, when run,
     *                    prints its output and all defined values in a comment column.
     */
    private def instrumentation(source: SourceFile, line: Int): (String, Array[Char]) = {
      val tree = global.typedTree(source, forceReload = true)
      val endOffset = if (line < 0) source.length else source.lineToOffset(line + 1)
      val patcher = new Patcher(source.content, new LexStructure(source), endOffset)
      patcher.traverse(tree)
      (patcher.objectName, patcher.result)
    }
  }

  /** Set sync var `response` to a pair consisting of
   *                  - the fully qualified name of the first top-level object definition in the file.
   *                    or "" if there are no object definitions.
   *                  - the text of the instrumented program which, when run,
   *                    prints its output and all defined values in a comment column.
   *
   *  @param source       The source file to be analyzed
   *  @param line         The line up to which results should be printed, -1 = whole document.
   *  @param response     The response
   */
  def askInstrumented(source: SourceFile, line: Int, response: Response[(String, Array[Char])]): Unit = try {
    compiler.withResponse[Unit] { compiler.askReload(List(source), _) }.get // just make sure it's loaded
    compiler.askOption { () => Instrumenter.askInstrumentation(source, line, response) }
  } finally {
    if (!response.isComplete) {
      logger.error("Result missing during instrumentation")
      response.raise(new MissingResponse)
    }
  }
}