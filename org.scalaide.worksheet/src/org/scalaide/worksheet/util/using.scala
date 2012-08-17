package org.scalaide.worksheet.util

import java.io.Closeable
import scala.util.control.Exception
import scala.util.control.Exception.Catch
import scala.util.control.Exception.ignoring

/** Automatic resource management, a.k.a. Loan pattern (the name is inspired by C#)*/
object using {
  def apply[T <: Closeable, R](resource: T)(body: T => R): R = apply(swallowException)(resource)(body)

  def apply[T <: Closeable, R](handlers: Catch[R])(resource: T)(body: T => R): R = (
    handlers
    andFinally (ignoring(classOf[Any]) { resource.close() })
    apply body(resource))
  
  private def swallowException = Exception.catchingPromiscuously(classOf[Exception])
}