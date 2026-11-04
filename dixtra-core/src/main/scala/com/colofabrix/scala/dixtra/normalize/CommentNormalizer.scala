package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import com.colofabrix.scala.dixtra.normalize.comments.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * Comment normalisation: comments reflowed to the maximum line width and scaladoc in the javadoc layout.
 *
 * Only comments on lines of their own change (a trailing comment after code stays as written, except for the space
 * after `//`):
 *
 *   - `//` comments, see [[comments.LineComments]].
 *   - `/* ... */` comments and scaladoc, see [[comments.BlockComments]].
 *   - Blank lines between a scaladoc and its definition are removed.
 *
 * The text of a comment is read as paragraphs and laid out again (see [[comments.Paragraphs]]). The pass is
 * token-aware; if the text cannot be tokenised, the input is returned unchanged (safety net).
 */
object CommentNormalizer {

  private val blankRun =
    """\A\n((?:[ \t]*\n)+)""".r

  /**
   * Normalise the comments of `source` for the given maximum line width.
   */
  def normalize(source: String, maxColumn: Int, alignTags: Boolean): String =
    try {
      SourceEdits
        .tokens(source)
        .fold(source) { tokens =>
          val text     = new CommentSource(source)
          val comments = tokens.collect { case c: Token.Comment => c }

          val edits =
            LineComments.edits(text, comments, maxColumn) ++
            BlockComments.edits(text, comments, maxColumn, alignTags) ++
            docGaps(text, comments)

          SourceEdits.applyEdits(source, edits)
        }
    } catch {
      case NonFatal(_) => source
    }

  // The blank lines between a scaladoc and the definition after it, unless what follows is not a definition.
  private def docGaps(text: CommentSource, comments: Vector[Token.Comment]): List[Edit] =
    comments
      .toList
      .filter(comment => SourceEdits.isScaladoc(comment.text) && text.ownLine(comment))
      .flatMap { comment =>
        val eol  = text.lineEnd(comment.end)
        val rest = text.source.drop(eol)
        blankRun
          .findPrefixMatchOf(rest)
          .flatMap { blankMatch =>
            val next     = rest.drop(blankMatch.end).dropWhile(_.isWhitespace)
            val separate = List("//", "/*", "package", "import", "}").exists(next.startsWith) || next.isEmpty
            Option.when(!separate) {
              Edit(eol + 1, eol + blankMatch.end, "")
            }
          }
      }

}
