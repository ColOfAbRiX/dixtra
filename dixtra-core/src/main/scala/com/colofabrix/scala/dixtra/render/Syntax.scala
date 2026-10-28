package com.colofabrix.scala.dixtra.render

import scala.meta.*

/**
 * Small syntax helpers shared by the renderers.
 */
private[render] object Syntax {

  /**
   * The keyword that opens a parameter or argument clause, followed by a space (`using ` / `implicit `), or the empty
   * string for a plain clause.
   */
  def clauseKeyword(mod: Option[Mod]): String =
    mod match {
      case Some(_: Mod.Using)    => "using "
      case Some(_: Mod.Implicit) => "implicit "
      case _                     => ""
    }

}
