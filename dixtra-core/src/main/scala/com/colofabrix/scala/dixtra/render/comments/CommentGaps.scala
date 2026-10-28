package com.colofabrix.scala.dixtra.render.comments

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.context.RenderContext

/**
 * The gaps between the parts of a construct (e.g. between the condition, `then` branch and `else` of an if/else), laid
 * out as the author wrote them.
 */
private[render] object CommentGaps {

  /**
   * The separator between two parts of a construct, reproducing the author's layout of the gap `[start, end)`: its
   * comments in order, each on the same line as what precedes it or on a new line, and a final line break or space
   * before the next part. Keywords in the gap (e.g. `then`) are emitted by the caller.
   */
  def separator(ctx: RenderContext, start: Int, end: Int): Doc =
    val comments = ctx.commentsBetween(start, end).toList
    val (doc, cursor) =
      comments.foldLeft((Doc.Empty: Doc, start)) { case ((acc, from), c) =>
        (acc ++ lineOrSpace(ctx, from, c.start) ++ ctx.sourceDoc(c.start, c.end), c.end)
      }
    doc ++ lineOrSpace(ctx, cursor, end)

  // A line break when the author broke the line within `[start, end)`.
  private def lineOrSpace(ctx: RenderContext, start: Int, end: Int): Doc =
    if ctx.slice(start, end).contains('\n') then Doc.Line else Doc.text(" ")

}
