package com.colofabrix.scala.dixtra.model

/**
 * Where `)(` goes between the groups of parameters or arguments of a broken definition or call.
 */
enum GroupsSeparator(val key: String) {

  /**
   * At the start of the next group, on the line of the closing `)`: `)(numeric: Numeric[A],`.
   */
  case NextGroup extends GroupsSeparator("next-group")

  /**
   * At the end of the last element of the group before: `second: Boolean)(`, the elements of the next group below it.
   */
  case PreviousGroup extends GroupsSeparator("previous-group")

}

/**
 * Whether the last element of a list laid out one element per line is followed by a comma.
 */
enum TrailingCommas(val key: String) {

  /**
   * A comma after the last element too.
   */
  case Always extends TrailingCommas("always")

  /**
   * No comma after the last element.
   */
  case Never extends TrailingCommas("never")

}

/**
 * The layout of signatures (`def`, class constructors) and of the other lists of names a definition declares: type
 * parameters and import selectors.
 *
 * @param groupsSeparator Where `)(` goes between broken parameter groups.
 * @param trailingCommas Whether the last parameter, type parameter or import selector laid out one per line is
 *                       followed by a comma.
 */
final case class DefinitionSiteConfig(
  groupsSeparator: GroupsSeparator,
  trailingCommas: TrailingCommas,
)

/**
 * Which argument groups of a call (`f(a)(b)`) break when they do not fit on one line.
 */
enum GroupsBreak(val key: String) {

  /**
   * Every group, each argument on its own line.
   */
  case BreakAll extends GroupsBreak("break-all")

  /**
   * Only the last group while the groups before it fit on the line of the call (`Option.when(cond)(` and the body
   * below); every group when they do not.
   */
  case LastFirst extends GroupsBreak("last-first")

  /**
   * Each group by itself, as for a call with one group: a group breaks when the author broke it or it does not fit from
   * where it starts.
   */
  case Independent extends GroupsBreak("independent")

}

/**
 * The layout of calls, constructor calls and tuples.
 *
 * @param groupsSeparator Where `)(` goes between broken argument groups.
 * @param groupsBreak Which argument groups break.
 * @param trailingCommas Whether the last argument or tuple element laid out one per line is followed by a comma.
 */
final case class CallSiteConfig(
  groupsSeparator: GroupsSeparator,
  groupsBreak: GroupsBreak,
  trailingCommas: TrailingCommas,
)
