package org.scalaide.worksheet.testutil

import org.scalaide.worksheet.editor.DocumentHolder
import org.scalaide.worksheet.runtime._

object EvalTester {
  import WorksheetUtils._
  
  class EditorProxy(initialContents: String) extends DocumentHolder {
    @volatile
    private var doc = initialContents
    @volatile
    private var done = false
    
    override def beginUpdate() { done = false }
    
    override def getContents = doc

    override def replaceWith(newDoc: String, revealOffset: Int) {
      doc = newDoc
    }
    
    override def endUpdate() {
      done = true
    }
    
    def isDone(): Boolean = done
  }
  
  import WorksheetRunner._
  
  /** Block until the given unit is evaluated. Wait `timeout` millis. */
  def runEvalSync(name: String, contents: String, timeout: Int = 10000): String = {
    val timeslice = 200 // ms
    var waited = 0
    val unit = getUnit(name, contents)
    val proxy = new EditorProxy(contents)

    WorksheetsManager.Instance ! RunEvaluation(unit, proxy)

    while (waited < timeout && !proxy.isDone) {
      Thread.sleep(timeslice)
      waited += timeslice
    }
    
    WorksheetsManager.Instance ! ProgramExecutor.StopRun(unit)
    
    proxy.getContents
  }
}
