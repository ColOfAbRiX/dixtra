package com.colofabrix.scala.dixtra.render

import scala.meta.*

/**
 * The safety-net fallback: for any AST node the engine does not yet know how to format, or cannot format without losing
 * information, the exact original text is reproduced from its source position rather than guessed. Coverage then grows
 * incrementally: fallback fires -> add that node's layout rule.
 *
 * Renderings that are produced are checked by [[com.colofabrix.scala.dixtra.verify.Equivalence]].
 */
object SafetyNet {

  /**
   * Return the exact original source text spanned by the given tree.
   */
  def verbatim(tree: Tree): String =
    tree.pos.text

}
