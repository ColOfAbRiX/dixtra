package com.colofabrix.scala.dixtra.testkit

import scala.meta.*

/**
 * Parsing for the specs that work on trees.
 */
object Parsing {

  /**
   * The tree of `text`, a Scala 3 source that must parse.
   */
  def parse(text: String): Source =
    given Dialect = dialects.Scala3
    text.parse[Source].get

}
