package com.colofabrix.scala.dixtra.testkit

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Registers a table of layouts as tests: each row names what it lays out, the configuration, the input and the expected
 * output.
 */
trait LayoutRows extends FormatMatchers { this: AnyWordSpec & Matchers =>

  /**
   * One test per row, and one that checks the expected output of a row is stable. A row whose input is its expected
   * layout already checks it is stable, and rows with the same expected layout and configuration check it once.
   */
  def layoutRows(rows: List[(String, Config, String, String)]): Unit =
    rows
      .zipWithIndex
      .foreach {
        case ((what, config, input, expected), index) =>
          s"lay out $what (row ${index.toString})" in {
            input should formatTo(expected, config)
          }
      }

    rows
      .zipWithIndex
      .filter { case ((_, _, input, expected), _) =>
        input != expected
      }
      .distinctBy { case ((_, config, _, expected), _) =>
        (config, expected)
      }
      .foreach { case ((what, config, _, expected), index) =>
        s"be idempotent for $what (row ${index.toString})" in {
          expected should formatTo(expected, config)
        }
      }

}
