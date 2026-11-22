package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.GroupsSeparator
import com.colofabrix.scala.dixtra.render.comments.CommentedList

/**
 * The groups of parameters or arguments of a broken definition or call, `(a, b)(c)`, each element on its own line.
 * Between groups the parentheses are glued by the groups separator: at the start of the next group (`)(c` for a plain
 * group, `)(using` for a `using` group) or after the last element of the group before (`b)(`, unless a comment follows
 * that element). A comment that prevents gluing on both sides can leave a lonely delimiter line. Empty groups stay
 * glued as `()`. In a definition the `using` / `implicit` keyword behaves exactly like a parameter.
 */
private[render] object ClauseGroups {

  /**
   * A group: the type clause that opens it (`[B]` in `def f[A](a: A)[B](b: B)`, empty for most groups), its keyword
   * (`using ` / `implicit ` / empty), its elements as documents, and the comments between them.
   */
  final case class Group(opening: String, keyword: String, elements: List[Doc], comments: CommentedList) {

    /**
     * The group on one line, with its parentheses.
     */
    def inline: Doc =
      Doc.text(s"$opening($keyword") ++ comments.inline(elements) ++ Doc.text(")")

  }

  /**
   * Broken groups: the document, and whether its last group is still open, its `)` not written yet (the caller writes
   * it on a line of its own); when none is open, `lastWidth` is the width of its last line.
   */
  final case class Layout(doc: Doc, open: Boolean, lastWidth: Int)

  /**
   * How a group is laid out by the caller that decides it: on one line with its own parentheses, broken with its
   * parentheses around it, or kept as the author wrote it.
   */
  sealed trait Arrangement {

    /**
     * The document of the group, without the closing delimiter of a broken group: the caller glues the `)(` of the
     * next group to it.
     *
     * @param closes True when the closing delimiter follows the last element of the group on its line.
     */
    def render(closes: Boolean): Doc

  }

  /**
   * A group on one line, with its own parentheses.
   */
  final case class OnOneLine(value: Doc) extends Arrangement {
    def render(closes: Boolean): Doc = value
  }

  /**
   * A group broken with its elements on their own lines, its opening parenthesis written and its closing one not: the
   * caller writes it, glued to the next group when the separator asks for it.
   *
   * @param group The group: its keyword and its elements, with their comments.
   * @param keywordAsParameter True when the keyword of the group goes on a line of its own, like a parameter of a
   *                            definition; else it stays after the `(`.
   */
  final case class Broken(group: Group, keywordAsParameter: Boolean) extends Arrangement {

    /**
     * The document of the group, its elements one per line, without the comma after the last one when `closes`: the
     * closing delimiter then follows that element on its line.
     */
    def render(closes: Boolean): Doc =
      val opening = Doc.text(group.opening + "(")
      val content =
        if keywordAsParameter then
          keywordOnOwnLine(group, closes)
        else
          openContent(group, closes)

      opening ++ content
  }

  /**
   * A group kept as the author wrote it: it closes itself and cannot host a glued `)(`.
   */
  final case class AsWritten(value: Doc) extends Arrangement {
    def render(closes: Boolean): Doc = value
  }

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
          val closes   = closesOnLast(group, next, separator)
          val parens   = Doc.text(group.opening + "(")
          val junction = Doc.text(")" + group.opening + "(")
          val empty    = group.opening + "()"
          (group.elements, open) match {
            case (Nil, None)      => (acc ++ Doc.text(empty), None, width + empty.length)
            case (Nil, Some(_))   => (acc ++ Doc.Line ++ Doc.text(")" + empty), None, baseIndent + 1 + empty.length)
            case (_, None)        => (acc ++ parens ++ opened(group, closes), Some(closes), 0)
            case (_, Some(true))  => (acc ++ junction ++ opened(group, closes), Some(closes), 0)
            case (_, Some(false)) => (acc ++ Doc.Line ++ junction ++ gluedContent(group, closes), Some(closes), 0)
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

  // A group opened via `)(` at the start of a line: a keyword group, or one opened by a type clause (`)[B](`), keeps
  // its opening on that line and nests its elements; a plain group glues its first element onto the `)(` line.
  private def gluedContent(group: Group, closes: Boolean): Doc =
    if group.keyword.nonEmpty || group.opening.nonEmpty then
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

  /**
   * The groups of a call, each with the layout the caller decided for it, laid out one after the other with the `)(`
   * between them glued so no lonely delimiter line appears.
   *
   * The groups break together here only in the sense that their junctions are glued: each group keeps the layout the
   * caller gave it, inline or broken. A broken group whose last element can host the junction closes on that element
   * (`b)(`), the one before it closing on the line of its own (`)`) and the next group opening there (`)(c`); a group
   * whose last element is followed by a comment, or kept as the author wrote it, cannot host the junction and the next
   * group opens after its closing delimiter.
   *
   * @param lead The document the groups follow.
   * @param groups The groups and their layout, in order.
   * @param separator Where the `)(` goes between the groups.
   * @param baseIndent The indent of the lines the groups are broken on.
   */
  def arranged(lead: Doc, groups: List[Arrangement], separator: GroupsSeparator, baseIndent: Int): Doc = {
    def closeBroken(run: List[Broken]): Doc =
      val brokenGroups = run.map(_.group)
      val layout       = broken(Doc.Empty, brokenGroups, separator, baseIndent, keywordAsParameter = false)

      if layout.open then
        layout.doc ++ Doc.Line ++ Doc.text(")")
      else
        layout.doc

    val (result, run) =
      groups.foldLeft((lead, List.empty[Broken])) { case ((doc, run), arrangement) =>
        arrangement match {
          case broken: Broken =>
            (doc, run :+ broken)

          case other =>
            val closed = if run.isEmpty then doc else doc ++ closeBroken(run)
            (closed ++ other.render(closes = false), Nil)
        }
      }

    if run.isEmpty then result else result ++ closeBroken(run)
  }

}
