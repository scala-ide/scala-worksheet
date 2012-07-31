package org.scalaide.worksheet

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.scalaide.worksheet.completion.CompletionTests

@RunWith(classOf[Suite])
@Suite.SuiteClasses(Array(
  classOf[WorksheetEvalTest],
  classOf[CompletionTests]
))
class TestSuite