package org.scalaide.worksheet.cross

import org.eclipse.core.resources.IFolder
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.scalaide.core.internal.project.ScalaProject
import org.scalaide.core.testsetup.SDTTestUtils.createProjects
import org.scalaide.core.testsetup.SDTTestUtils.deleteProjects
import org.scalaide.util.internal.SettingConverterUtil

object Scala210VersionTest {
  @BeforeClass
  def createProject() {
    val Seq(prj) = createProjects("version-210-test")
    project = prj.asInstanceOf[ScalaProject]
    val projectSpecificStorage = project.projectSpecificStorage
    projectSpecificStorage.setValue(SettingConverterUtil.SCALA_DESIRED_INSTALLATION, "2.10.6")

    project.sourceOutputFolders.collect {
      case (_, outp: IFolder) => outp.create(true, true, null)
    }
  }

  var project: ScalaProject = _

  @AfterClass
  def deleteProject() {
    deleteProjects(project)
  }
}

class Scala210VersionTest {
  import Scala210VersionTest._

  @Test
  def runScala210withSuccess(): Unit = {
    evaluate(project, 2, 10, 6)
  }
}
