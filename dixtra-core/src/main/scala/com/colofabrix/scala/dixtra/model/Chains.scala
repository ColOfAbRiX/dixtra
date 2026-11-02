package com.colofabrix.scala.dixtra.model

/**
 * Whether the first selections of a chain stay together with its receiver.
 */
enum ChainHeadPolicy(val key: String) {

  /**
   * Always together, even when the author breaks the line between them.
   */
  case AlwaysJoin extends ChainHeadPolicy("always-join")

  /**
   * Together unless the author breaks the line between them.
   */
  case Keep extends ChainHeadPolicy("keep")

  /**
   * Never together: they break like any other selection.
   */
  case AlwaysBreak extends ChainHeadPolicy("always-break")

}

/**
 * When select chains break.
 *
 * @param breakAtCount A chain with at least this many selections breaks one selection per line when the author writes
 *                     it below `=` / `=>`.
 * @param namespacePath The lowercase names of a chain up to and including the first capitalised one
 *                      (`java.nio.file.Files`).
 * @param objectMember The first member selected on a capitalised name (`Files.exists`).
 * @param receiverMinLength A chain breaks after a receiver name at least this long; a shorter one keeps its first
 *                          selection on its line (`v.trim`), and that selection still counts towards `breakAtCount`.
 * @param inlineChains Whether a chain on the line of its `=` / `=>` breaks at `breakAtCount` selections too.
 * @param inFor Whether a chain in the enumerators of a `for` keeps the line breaks of its author.
 * @param inCall Whether a chain in the arguments of a call keeps the line breaks of its author.
 */
final case class SelectorChainConfig(
  breakAtCount: Int,
  namespacePath: ChainHeadPolicy,
  objectMember: ChainHeadPolicy,
  receiverMinLength: Int,
  inlineChains: InlineChainPolicy,
  inFor: NestedChainPolicy,
  inCall: NestedChainPolicy,
)

/**
 * How a chain nested where [[SelectorChainConfig.breakAtCount]] does not apply breaks: in the enumerators of a `for`
 * (its generators, guards and value definitions, and what they hold, but not the `yield` expression or the `do` body),
 * and in the arguments of a call, positional or named (but not the bodies of the lambdas and blocks passed to it). A
 * chain there never breaks at `breakAtCount` selections, and always breaks when it does not fit.
 */
enum NestedChainPolicy(val key: String) {

  /**
   * On one line while it fits, even when the author broke it.
   */
  case NeverBreak extends NestedChainPolicy("never-break")

  /**
   * Broken where the author broke it, like any other chain.
   */
  case Keep extends NestedChainPolicy("keep")

}

/**
 * Whether a chain on the line of its `=` / `=>` breaks at [[SelectorChainConfig.breakAtCount]] selections.
 */
enum InlineChainPolicy(val key: String) {

  /**
   * On one line while it fits.
   */
  case Keep extends InlineChainPolicy("keep")

  /**
   * One selection per line from `break-at-count` selections, like a chain below the `=` / `=>`.
   */
  case BreakAtCount extends InlineChainPolicy("break-at-count")

}
