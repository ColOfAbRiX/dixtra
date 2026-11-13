package com.colofabrix.scala.dixtra.normalize.comments

import com.colofabrix.scala.dixtra.normalize.SourceEdits
import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import com.colofabrix.scala.dixtra.normalize.comments.Paragraphs.Para
import com.colofabrix.scala.dixtra.normalize.comments.Paragraphs.Para.*
import scala.meta.tokens.Token

/**
 * The `/* ... */` comments and the scaladoc, on lines of their own.
 *
 * A one-line block comment past the width gets newlines, its continuation lines aligned under the text after the
 * opening delimiter, and a multi-line one is reflowed with its own gutter. A scaladoc gets its delimiters on lines of
 * their own and a ` * ` gutter, without blank lines at its edges, with one blank line before its first tag, reflowed
 * with tags continuing under their description (aligned on one column for the whole tag list with `alignTags`). A
 * one-line scaladoc that fits stays as it is.
 */
private[normalize] object BlockComments {

  /**
   * The edits of the block comments among `comments`.
   */
  def edits(text: CommentSource, comments: Vector[Token.Comment], maxColumn: Int, alignTags: Boolean): List[Edit] =
    comments
      .toList
      .filter { comment => comment.text.startsWith("/*") && text.ownLine(comment) }
      .flatMap { comment =>
        if SourceEdits.isScaladoc(comment.text) then
          scaladoc(text, comment, maxColumn, alignTags)
        else
          plainBlock(text, comment, maxColumn)
      }

  private def scaladoc(text: CommentSource, comment: Token.Comment, maxColumn: Int, alignTags: Boolean): Option[Edit] =
    val col         = text.column(comment.start)
    val oneLine     = !comment.text.contains('\n')
    val contents    = docContents(comment.text, col)
    val oneLineFits = oneLine && text.lineWidth(comment.start) <= maxColumn
    val hasBanner   = contents.exists(Paragraphs.isBanner)
    val untouched   = oneLineFits || hasBanner
    val paras       = docParas(Paragraphs.parse(contents, withTags = true))
    val width       = maxColumn - col - 3

    Option
      .when(!untouched)(Paragraphs.render(paras, width, alignTags))
      .filter(_.nonEmpty)
      .map(docLines(_, col))
      .filter(_ != comment.text)
      .map(Edit(comment.start, comment.end, _))

  // The rendered lines of a scaladoc at `col`, with its delimiters and its gutter.
  private def docLines(lines: Vector[String], col: Int): String =
    val indent = " " * col
    val body   = lines.map(line => if line.isEmpty then indent + " *" else indent + " * " + line)
    ("/**" +: body :+ (indent + " */")).mkString("\n")

  // The text lines of a scaladoc without delimiters and gutter: the text on the `/**` line (Scala style) first, then
  // each line without its ` * `.
  private def docContents(comment: String, col: Int): Vector[String] =
    val lines =
      comment
        .stripPrefix("/**")
        .stripSuffix("*/")
        .split("\n", -1)
        .toVector

    val first =
      lines
        .headOption
        .map(_.trim)
        .toVector

    val rest =
      lines
        .drop(1)
        .map(withoutGutter(_, col))

    first ++ rest

  private def withoutGutter(line: String, col: Int): String =
    val trimmed = line.dropWhile(_ == ' ')
    if trimmed.startsWith("*") then
      val content = trimmed.drop(1)
      val text    = if content.startsWith(" ") then content.drop(1) else content
      text.stripTrailing
    else
      line
        .drop(math.min(SourceEdits.indent(line), col + 3))
        .stripTrailing

  // No blank lines at the edges, and one blank line before the first tag.
  private def docParas(paras: Vector[Para]): Vector[Para] =
    val trimmed  = withoutEdgeBlanks(paras)
    val firstTag = trimmed.indexWhere { case _: Tag => true; case _ => false }

    if firstTag <= 0 then
      trimmed
    else
      val description = withoutEdgeBlanks(trimmed.take(firstTag))
      val tags        = trimmed.drop(firstTag)
      description ++ Vector(Blank) ++ tags

  private def withoutEdgeBlanks(paras: Vector[Para]): Vector[Para] =
    paras
      .dropWhile(_ == Blank)
      .reverse
      .dropWhile(_ == Blank)
      .reverse

  private def plainBlock(text: CommentSource, comment: Token.Comment, maxColumn: Int): Option[Edit] =
    val col = text.column(comment.start)
    if !comment.text.contains('\n') then
      Option.when(text.lineWidth(comment.start) > maxColumn) {
        val words = Paragraphs.split(comment.text.stripPrefix("/*").stripSuffix("*/")) :+ "*/"
        val lines = Paragraphs.wrap(words, "/* ", "   ", maxColumn - col)
        Edit(comment.start, comment.end, lines.mkString("\n" + " " * col))
      }
    else
      multiLineBlock(comment, col, maxColumn)

  // A multi-line `/* ... */` reflowed with its own gutter (` * ` lines) or, without one, with the indent of its lines.
  // The opening line keeps its text unless the text lines continue under it; a `*/` on its own line stays there.
  private def multiLineBlock(comment: Token.Comment, col: Int, maxColumn: Int): Option[Edit] =
    val lines =
      comment
        .text
        .split("\n", -1)
        .toVector

    val opener =
      lines
        .headOption
        .getOrElse("")
        .stripPrefix("/*")

    val closing = lines.lastOption.getOrElse("")
    val tail    = closing.stripSuffix("*/")
    val middle  = lines.drop(1).dropRight(1) ++ Vector(tail).filter(_.trim.nonEmpty)
    val shape   = BlockShape.of(opener, middle.filter(_.trim.nonEmpty), col)
    val first   = if shape.joinsOpener then Vector(opener.trim) else Vector.empty
    val texts   = first ++ middle.map(shape.content(_, col))

    val closeInline = tail.trim.nonEmpty
    val paras       = Paragraphs.parse(texts, withTags = false)
    val closer      = if closeInline then withCloser(paras) else None
    val rendered    = Paragraphs.render(closer.getOrElse(paras), maxColumn - shape.textColumn, alignTags = false)
    val body        = rendered.zipWithIndex.map(shape.line)
    val head        = if shape.joinsOpener then Vector.empty else Vector("/*" + opener.stripTrailing)

    val closed =
      if closer.isDefined then
        body
      else if closeInline then
        body.dropRight(1) ++ body.lastOption.map(_ + " */")
      else
        body :+ closing

    val out       = (head ++ closed).mkString("\n")
    val hasBanner = texts.exists(Paragraphs.isBanner)
    val reflowed  = rendered.nonEmpty && !hasBanner && out != comment.text

    Option.when(reflowed) {
      Edit(comment.start, comment.end, out)
    }

  // How the text lines of a multi-line block comment are laid out: after a gutter (`prefix`, ending with `*`) or at a
  // margin (`prefix`, only spaces), the first one on the opening line when `joinsOpener`.
  private final case class BlockShape(gutter: Boolean, prefix: String, margin: Int, joinsOpener: Boolean) {

    // The column where the text starts.
    def textColumn: Int =
      prefix.length + (if gutter then 1 else 0)

    // The text of a source line, without the gutter or the margin.
    def content(line: String, col: Int): String =
      if gutter then withoutGutter(line, col)
      else line.drop(math.min(SourceEdits.indent(line), margin)).stripTrailing

    // The output line of the `index`-th rendered text line.
    def line(text: String, index: Int): String =
      if joinsOpener && index == 0 then
        "/* " + text
      else if text.isEmpty then
        (if gutter then prefix else "")
      else if gutter then
        prefix + " " + text
      else
        prefix + text

  }

  private object BlockShape {

    // The shape of a comment at `col`, by its non-blank text lines `filled`.
    def of(opener: String, filled: Vector[String], col: Int): BlockShape =
      val gutter = filled.nonEmpty && filled.forall(_.trim.startsWith("*"))

      val margin =
        filled
          .map(SourceEdits.indent)
          .minOption
          .getOrElse(col + 3)

      val prefix =
        if gutter then
          filled
            .headOption
            .map(l => l.takeWhile(_ == ' ') + "*")
            .getOrElse(" " * col + " *")
        else
          " " * margin

      val textUnderOpener =
        opener.trim.nonEmpty && !gutter && margin == col + 3

      BlockShape(gutter, prefix, margin, textUnderOpener)

  }

  // The paragraphs with the closing delimiter as the last word of the last one, so it wraps like any word, when that
  // paragraph is text.
  private def withCloser(paras: Vector[Para]): Option[Vector[Para]] =
    paras
      .lastOption
      .collect {
        case Prose(l, ws)   => paras.dropRight(1) :+ Prose(l, ws :+ "*/")
        case Item(l, m, ws) => paras.dropRight(1) :+ Item(l, m, ws :+ "*/")
      }

}
