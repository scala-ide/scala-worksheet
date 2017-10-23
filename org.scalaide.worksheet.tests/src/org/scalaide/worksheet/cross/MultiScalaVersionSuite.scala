package org.scalaide.worksheet.cross

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(classOf[Suite])
@Suite.SuiteClasses(Array(
  classOf[Scala211VersionTest],
  classOf[Scala212VersionTest],
  classOf[Scala210VersionTest]
))
class MultiScalaVersionSuite
