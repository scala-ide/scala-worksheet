package org.scalaide.worksheet

import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.NoPosition
import scala.reflect.internal.util.Position
import scala.tools.nsc.io.AbstractFile

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.jface.text.IDocument
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.part.FileEditorInput
import org.scalaide.core.IScalaPlugin
import org.scalaide.core.compiler.ISourceMap
import org.scalaide.core.compiler.InteractiveCompilationUnit
import org.scalaide.core.resources.EclipseResource
import org.scalaide.core.resources.MarkerFactory
import org.scalaide.worksheet.WorksheetMarkers.BuildProblemMarker
import org.scalaide.worksheet.editor.ScriptEditor

/** A Script compilation unit connects the presentation compiler
 *  view of a script with the Eclipse IDE view of the underlying
 *  resource.
 */
case class ScriptCompilationUnit(val workspaceFile: IFile) extends InteractiveCompilationUnit {

  @volatile private var document: Option[IDocument] = None
  @volatile private var lastInfo: ISourceMap = _

  override def file: AbstractFile = EclipseResource(workspaceFile)

  override lazy val scalaProject = IScalaPlugin().asScalaProject(workspaceFile.getProject).get

  override def lastSourceMap(): ISourceMap = {
    if (lastInfo eq null)
      lastInfo = sourceMap(getContents())
    lastInfo
  }

  override def sourceMap(contents: Array[Char]): ISourceMap = {
    lastInfo = ISourceMap.plainScala(file, contents)
    lastInfo
  }

  /** Return the compiler ScriptSourceFile corresponding to this unit. */
  def batchSourceFile(contents: Array[Char]): BatchSourceFile = {
    new BatchSourceFile(file, contents)
  }

  override def exists(): Boolean = true

  override def getContents: Array[Char] = document.map(_.get.toCharArray).getOrElse(file.toCharArray)

  def connect(doc: IDocument): this.type = {
    document = Option(doc)
    this
  }

  def clearBuildErrors(): Unit = {
    WorksheetMarkers.clearBuildErrors(workspaceFile, null)
  }

  /** Build errors need to be mapped back to the original source. Right now
   *  we don't have the necessary information. The build compiler reports
   *  positions relative to the instrumented source, but offsets are skewed.
   *
   *  We rely on the fact that the instrumenter never inserts newlines, so we
   *  report the error only against the line number, with a length of one.
   */
  def reportBuildError(errorMsg: String, position: Position): Unit = {
    BuildProblemMarker.create(workspaceFile, IMarker.SEVERITY_ERROR, errorMsg, worksheetPosition(position))
  }

  /** A simple heuristic to map back positions to worksheet position.
   *
   *  The assumption is that the line numbers match. We can't use column information
   *  because instrumented code may appear on the same line before the error point.
   */
  private def worksheetPosition(pos: Position): MarkerFactory.RegionPosition = {
    val sourceFile = batchSourceFile(getContents)
    val line = if (pos == NoPosition) 1 else pos.line - 1
    val length = sourceFile.lineToString(line).length
    val offset = sourceFile.lineToOffset(line)
    MarkerFactory.RegionPosition(offset, length, line)
  }
}

object ScriptCompilationUnit {
  def fromEditor(scriptEditor: ScriptEditor): ScriptCompilationUnit = {
    val input = scriptEditor.getEditorInput
    if(input == null) throw new NullPointerException("No edito input for editor " + scriptEditor + ". Hint: Maybe the editor isn't yet fully initialized?")
    else {
      val unit = fromEditorInput(input)
      unit.connect(scriptEditor.getDocumentProvider().getDocument(input))
    }
  }

  private def fromEditorInput(editorInput: IEditorInput): ScriptCompilationUnit = ScriptCompilationUnit(getFile(editorInput))

  private def getFile(editorInput: IEditorInput): IFile =
    editorInput match {
      case fileEditorInput: FileEditorInput if fileEditorInput.getName.endsWith(ScriptEditor.SCRIPT_EXTENSION) =>
        fileEditorInput.getFile
      case _ => null
    }
}
