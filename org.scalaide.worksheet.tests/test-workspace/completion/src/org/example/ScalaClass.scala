package org.example

/** Copied from sdt.core.tests, just to smoke-test completions. */
class Ticket1000475 {
  val v = new Object
  v.toS /*!*/
  
  val m = Map(1 -> "1")
  m(1).foral /*!*/
  println()
  
  m(1) foral /*!*/ /** infix notation also works fine */
  println()
}
