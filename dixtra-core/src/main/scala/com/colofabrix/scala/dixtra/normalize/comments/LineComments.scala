package com.colofabrix.scala.dixtra.normalize.comments

import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import scala.meta.tokens.Token

/**
 * The `//` comments: consecutive own-line `//` comments at one column are one comment, reflowed to the width, with at
 * least one space after `//`. A trailing comment only gets that space. Directives (`///`, `//>`, `//noinspection`, ...)
 * are left alone.
 */
private[normalize] object LineComments {

  // `///` and `//>` directly after `//`, and tool directives after optional spaces.
  private val directivePrefixes = List("/", ">")

  private val toolDirectives =
    List("noinspection", "scalastyle:", "scalafmt:", "format:", "$COVERAGE", "#region", "#endregion")

  /**
   * The edits of the `//` comments among `comments`.
   */
  def edits(text: CommentSource, comments: Vector[Token.Comment], maxColumn: Int): List[Edit] =
    val singles         = comments.filter(comment => comment.text.startsWith("//") && !isDirective(comment))
    val (own, trailing) = singles.partition(text.ownLine)

    val spacing =
      trailing
        .toList
        .flatMap { comment =>
          val withSpace = spaced(comment.text)
          Option.when(withSpace != comment.text)(Edit(comment.start, comment.end, withSpace))
        }

    val reflowed = runs(text, own).flatMap(reflow(text, _, maxColumn))

    spacing ++ reflowed

  private def isDirective(comment: Token.Comment): Boolean =
    val content = comment.text.drop(2)
    directivePrefixes.exists(content.startsWith) || toolDirectives.exists(content.trim.startsWith)

  private def spaced(comment: String): String =
    val content = comment.drop(2)
    if content.isEmpty || content.headOption.exists(_.isWhitespace) then
      comment
    else
      "// " + content

  // Runs of own-line `//` comments on consecutive lines at the same column.
  private def runs(text: CommentSource, comments: Vector[Token.Comment]): List[Vector[Token.Comment]] =
    comments
      .foldLeft(List.empty[Vector[Token.Comment]]) { (acc, comment) =>
        acc match {
          case current :: rest if current.lastOption.exists(adjacent(text, _, comment)) =>
            (current :+ comment) :: rest
          case _ =>
            Vector(comment) :: acc
        }
      }
      .reverse

  private def adjacent(text: CommentSource, a: Token, b: Token): Boolean =
    val gap          = text.source.slice(a.end, b.start)
    val oneLineApart = gap.isBlank && gap.count(_ == '\n') == 1
    val sameColumn   = text.column(a.start) == text.column(b.start)
    oneLineApart && sameColumn

  private def reflow(text: CommentSource, group: Vector[Token.Comment], maxColumn: Int): Option[Edit] =
    for
      first   <- group.headOption
      last    <- group.lastOption
      col      = text.column(first.start)
      contents = group.map(comment => spaced(comment.text).drop(2).stripTrailing)
      paras    = Paragraphs.parse(contents, withTags = false)
      lines    = Paragraphs.render(paras, maxColumn - col - 2, alignTags = false)
      out      = lines.map("//" + _).mkString("\n" + " " * col) if out != text.source.slice(first.start, last.end)
    yield
      Edit(first.start, last.end, out)

}
