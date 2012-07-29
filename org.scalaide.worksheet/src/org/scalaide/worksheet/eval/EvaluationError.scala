package org.scalaide.worksheet.eval

import scala.tools.nsc.reporters.StoreReporter

sealed abstract class EvaluationError

case class CompilationError(reporter: StoreReporter) extends EvaluationError
case class InstrumentedError(ex: Throwable) extends EvaluationError
case class ExecutionError(message: Throwable) extends EvaluationError
