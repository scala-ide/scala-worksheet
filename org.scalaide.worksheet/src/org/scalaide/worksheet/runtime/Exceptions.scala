package org.scalaide.worksheet.runtime

import org.scalaide.worksheet.ScriptCompilationUnit

case class ProgramInstrumentationFailed(unit: ScriptCompilationUnit, exception: Throwable)
  extends RuntimeException("Error during askInstrumented of unit " + unit.file.name, exception)

case class MissingTopLevelObjectDeclaration(unit: ScriptCompilationUnit)
  extends RuntimeException("Couldn't find top level object declaration in source " + unit.file.name)