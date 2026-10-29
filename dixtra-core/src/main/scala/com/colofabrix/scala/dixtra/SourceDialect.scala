package com.colofabrix.scala.dixtra

import scala.meta.*

/**
 * The Scalameta dialect a whole source file is parsed with.
 */
object SourceDialect {

  /**
   * Scala 3 sources.
   */
  val scala: Dialect =
    dialects.Scala3

  /**
   * sbt build definitions: Scala 3 with statements at the top level (`name := "x"`), as sbt 2 reads them.
   */
  val sbt: Dialect =
    dialects.Scala3.withAllowToplevelTerms(true)

  /**
   * The Scala 2 counterpart of `dialect`, used only to recognise Scala 2 files: sbt 1 build definitions for [[sbt]],
   * Scala 2.13 otherwise.
   */
  def scala2(dialect: Dialect): Dialect =
    if dialect == sbt then dialects.Sbt1 else dialects.Scala213

  /**
   * The dialect of the file named `fileName`: sbt for `.sbt`, Scala 3 otherwise.
   */
  def of(fileName: String): Dialect =
    if fileName.endsWith(".sbt") then sbt else scala

}
