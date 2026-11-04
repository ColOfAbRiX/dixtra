package com.colofabrix.scala.dixtra.model

/**
 * What happens to a line of code that goes past the maximum width only because of the comment at its end.
 */
enum TrailingOverflow(val key: String) {

  /**
   * The line stays as it is: a trailing comment does not count in the width.
   */
  case Keep extends TrailingOverflow("keep")

  /**
   * The comment moves to a line of its own above the code, at its indent.
   */
  case MoveAbove extends TrailingOverflow("move-above")

}

/**
 * What happens to what goes past the maximum width.
 *
 * @param trailingComment What happens to a line that goes past the maximum width only because of its trailing comment.
 */
final case class OverflowConfig(
  trailingComment: TrailingOverflow,
)
