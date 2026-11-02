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
 * The layout of signatures (`def`, class constructors).
 *
 * @param groupsSeparator Where `)(` goes between broken parameter groups.
 */
final case class DefinitionSiteConfig(
  groupsSeparator: GroupsSeparator,
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
 * The layout of calls.
 *
 * @param groupsSeparator Where `)(` goes between broken argument groups.
 * @param groupsBreak Which argument groups break.
 */
final case class CallSiteConfig(
  groupsSeparator: GroupsSeparator,
  groupsBreak: GroupsBreak,
)
