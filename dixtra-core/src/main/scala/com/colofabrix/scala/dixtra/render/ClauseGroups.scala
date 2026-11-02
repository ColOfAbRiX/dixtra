package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.model.GroupsSeparator
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.comments.CommentedList

/**
 * The groups of parameters or arguments of a broken definition or call, `(a, b)(c)`, each element on its own line.
 * Between groups the parentheses are glued so no lonely delimiter line ever appears, by the groups separator: at the
 * start of the next group (`)(c` for a plain group, `)(using` for a `using` group) or after the last element of the
 * group before (`b)(`, unless a comment follows that element). Empty groups stay glued as `()`. In a definition the
 * `using` / `implicit` keyword behaves exactly like a parameter.
 */
private[render] object ClauseGroups {

  /**
   * A group: its keyword (`using ` / `implicit ` / empty), its elements as documents, and the comments between them.
   */
  final case class Group(keyword: String, elements: List[Doc], comments: CommentedList) {

    /**
     * The group on one line, with its parentheses.
     */
    def inline: Doc =
      Doc.text(s"($keyword") ++ comments.inline(elements) ++ Doc.text(")")

  }

  /**
   * Broken groups: the document, and whether its last group is still open, its `)` not written yet (the caller writes
   * it on a line of its own); when none is open, `lastWidth` is the width of its last line.
   */
  final case class Layout(doc: Doc, open: Boolean, lastWidth: Int)

  /**
   * The groups after `lead`, broken, on lines indented at `baseIndent`. With `keywordAsParameter` (a definition) the
   * `using` / `implicit` keyword is placed like a parameter, else (a call) it stays after its '('.
   */
  def broken(
    lead: Doc,
    groups: List[Group],
    separator: GroupsSeparator,
    baseIndent: Int,
    keywordAsParameter: Boolean,
  ): Layout =
    val following = groups.drop(1).map(Option(_)) :+ None

    // The content of a group opened at the end of a line: its keyword on a line of its own when it is placed like a
    // parameter, after the `(` otherwise.
    def opened(group: Group, closes: Boolean): Doc =
      if keywordAsParameter then
        keywordOnOwnLine(group, closes)
      else
        openContent(group, closes)

    // State: the document so far; when a group is still open (its `)` not yet written), whether `)(` follows its last
    // element; and the width of the current last line when no group is open.
    val (doc, open, lastWidth) =
      groups
        .zip(following)
        .foldLeft((lead, Option.empty[Boolean], 0)) { case ((acc, open, width), (group, next)) =>
          val closes = closesOnLast(group, next, separator)
          (group.elements, open) match {
            case (Nil, None)      => (acc ++ Doc.text("()"), None, width + 2)
            case (Nil, Some(_))   => (acc ++ Doc.Line ++ Doc.text(")()"), None, baseIndent + 3)
            case (_, None)        => (acc ++ Doc.text("(") ++ opened(group, closes), Some(closes), 0)
            case (_, Some(true))  => (acc ++ Doc.text(")(") ++ opened(group, closes), Some(closes), 0)
            case (_, Some(false)) => (acc ++ Doc.Line ++ Doc.text(")(") ++ gluedContent(group, closes), Some(closes), 0)
          }
        }

    Layout(doc, open.isDefined, lastWidth)

  /**
   * A broken group opened at the end of a line: its keyword (if any) stays on the opening line and every element is
   * nested on its own line, without the comma after the last one when `closes`.
   */
  def openContent(group: Group, closes: Boolean): Doc =
    Doc.text(group.keyword.trim) ++ Doc.Nest(2, Doc.Line ++ lines(group, closes))

  /**
   * A broken group opened at the end of a line, its keyword (if any) on a line of its own like a first parameter, and
   * every element nested on its own line, without the comma after teh last one when `closes`.
   */
  def keywordOnOwnLine(group: Group, closes: Boolean): Doc =
    if group.keyword.isEmpty then
      openContent(group, closes)
    else
      Doc.Nest(2, Doc.Line ++ Doc.text(group.keyword.trim) ++ Doc.Line ++ lines(group, closes))

  /**
   * The elements of a group with the first one glued to the current line and the rest one per line; nothing is glued
   * when the first element is preceded by a comment on its own line.
   */
  def gluedElements(group: Group, closes: Boolean): Doc =
    if group.elements.isEmpty then
      Doc.Empty
    else if group.comments.firstCanGlue then
      Doc.Nest(2, lines(group, closes))
    else
      Doc.Nest(2, Doc.Line ++ lines(group, closes))

  // Whether the `)(` before `next` follows the last element of `group`.
  private def closesOnLast(group: Group, next: Option[Group], separator: GroupsSeparator): Boolean =
    val afterPreviousGroup = separator == GroupsSeparator.PreviousGroup
    val hasNextGroup       = next.exists(_.elements.nonEmpty)

    val closedOnLast =
      group
        .comments
        .explodedClosedOnLast(group.elements)
        .isDefined

    afterPreviousGroup && hasNextGroup && closedOnLast

  // A group opened via `)(` at the start of a line: a keyword group keeps the keyword on that line and nests its
  // elements; a plain group glues its first element onto the `)(` line.
  private def gluedContent(group: Group, closes: Boolean): Doc =
    if group.keyword.nonEmpty then
      openContent(group, closes)
    else
      gluedElements(group, closes)

  // The elements of a group one per line, without the comma after the last one when `closes`.
  private def lines(group: Group, closes: Boolean): Doc =
    val exploded =
      group
        .comments
        .exploded(group.elements)

    if closes then
      group
        .comments
        .explodedClosedOnLast(group.elements)
        .getOrElse(exploded)
    else
      exploded

}
