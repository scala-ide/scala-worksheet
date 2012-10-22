package scala.tools.nsc.interactive

import scala.tools.nsc.util.SourceFile
import scala.tools.eclipse.ScalaPresentationCompiler
import scala.tools.eclipse.logging.HasLogger

final class ProgramInstrumenter(compiler: ScalaPresentationCompiler) extends HasLogger { self =>
  private object Instrumenter extends ScratchPadMaker2 {
    override protected val global: Global = self.compiler

    // Note: Reflection here is needed for compatibility with Scala IDE V2.1-M2.  
    private val setInterruptEnabled: Option[java.lang.reflect.Method] = {
      val mangledSetterName = "interruptsEnabled_$eq"
      val method = classOf[Global].getDeclaredMethods().find(_.getName contains mangledSetterName)
      method.foreach(_.setAccessible(true))
      method
    }

    def askInstrumentation(source: SourceFile, line: Int, response: Response[(String, Array[Char])]): Unit =
      try {
        setInterruptEnabled.foreach(_.invoke(global, Seq(false.asInstanceOf[Object]) :_*))
        global.respond(response) {
          instrumentation(source, line)
        }
      } finally {
        setInterruptEnabled.foreach(_.invoke(global, Seq(true.asInstanceOf[Object]) :_*))
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
      val patcher = new Patcher(source.content, new LexicalStructure(source), endOffset)
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