package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * Comment normalisation: comments reflowed to the maximum line width and scaladoc in the javadoc layout.
 *
 * Only comments on lines of their own change (a trailing comment after code stays as written, except for the space
 * after `//`):
 *
 *   - Consecutive `//` lines at the same column are one comment. Its paragraphs are joined and re-wrapped to fill the
 *     line, and an overlong single `//` line becomes several. There is at least one space after `//`; more are kept.
 *     Directives (`///`, `//>`, `//noinspection`, ...) are left alone.
 *   - A one-line block comment past the width gets newlines, its continuation lines aligned under the text after the
 *     opening delimiter. A multi-line block comment is reflowed with its own gutter.
 *   - A scaladoc gets its opening and closing delimiters on lines of their own and a ` * ` gutter, without blank lines
 *     at its edges, with one blank line before its first tag, reflowed with tags continuing under their description
 *     (aligned on one column for the whole tag list with `alignTags`). A one-line scaladoc that fits stays as it is.
 *     Blank lines between a scaladoc and its definition are removed.
 *
 * Blank lines, tags, list items, headings and tables start their own paragraph; code (fences, indented lines and lines
 * that look like code) and words longer than the line are never changed. A line never ends with a word that would make
 * it look like code, nor starts with one that would make it a list item or a tag, so the result is stable on a second
 * run.
 *
 * The pass is token-aware; if the text cannot be tokenised, the input is returned unchanged (safety net).
 */
object CommentNormalizer {

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
            lineComments(text, comments, maxColumn) ++
            blockComments(text, comments, maxColumn, alignTags) ++
            docGaps(text, comments)

          SourceEdits.applyEdits(source, edits)
        }
    } catch {
      case NonFatal(_) => source
    }

  // The lines of a comment, grouped by meaning.
  private enum Para {
    case Blank
    case Verbatim(line: String)
    case Prose(lead: Int, words: Vector[String])
    case Item(lead: Int, marker: String, words: Vector[String])
    case Tag(head: String, words: Vector[String])
  }
  import Para.*

  // The source, with the line queries on it the rules need.
  private final class CommentSource(val source: String) {

    def lineStart(offset: Int): Int =
      SourceEdits.lineStart(source, offset)

    def lineEnd(offset: Int): Int =
      SourceEdits.lineEnd(source, offset)

    def column(offset: Int): Int =
      offset - lineStart(offset)

    def lineWidth(offset: Int): Int =
      lineEnd(offset) - lineStart(offset)

    // Only whitespace before the comment on its first line and after it on its last.
    def ownLine(comment: Token): Boolean =
      source.slice(lineStart(comment.start), comment.start).isBlank &&
      source.slice(comment.end, lineEnd(comment.end)).isBlank

  }

  // ---- `//` comments

  // `///` and `//>` directly after `//`, and tool directives after optional spaces.
  private val directivePrefixes = List("/", ">")
  private val toolDirectives =
    List("noinspection", "scalastyle:", "scalafmt:", "format:", "$COVERAGE", "#region", "#endregion")

  private def lineComments(text: CommentSource, comments: Vector[Token.Comment], maxColumn: Int): List[Edit] =
    val singles         = comments.filter(comment => comment.text.startsWith("//") && !isDirective(comment))
    val (own, trailing) = singles.partition(text.ownLine)

    val spacing =
      trailing
        .toList
        .flatMap { comment =>
          val withSpace = spaced(comment.text)
          Option.when(withSpace != comment.text)(Edit(comment.start, comment.end, withSpace))
        }

    val reflowed = lineCommentRuns(text, own).flatMap(reflowLines(text, _, maxColumn))

    spacing ++ reflowed

  private def isDirective(comment: Token.Comment): Boolean =
    val content = comment.text.drop(2)
    directivePrefixes.exists(content.startsWith) || toolDirectives.exists(content.trim.startsWith)

  private def spaced(comment: String): String =
    val content = comment.drop(2)
    if content.isEmpty || content.headOption.exists(_.isWhitespace) then
      comment
    else
      "// " +
      content

  // Runs of own-line `//` comments on consecutive lines at the same column.
  private def lineCommentRuns(text: CommentSource, comments: Vector[Token.Comment]): List[Vector[Token.Comment]] =
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

  private def reflowLines(text: CommentSource, group: Vector[Token.Comment], maxColumn: Int): Option[Edit] =
    for
      first   <- group.headOption
      last    <- group.lastOption
      col      = text.column(first.start)
      contents = group.map(comment => spaced(comment.text).drop(2).stripTrailing)
      lines    = render(parse(contents, withTags = false), maxColumn - col - 2, alignTags = false)
      out      = lines.map("//" + _).mkString("\n" + " " * col)
                 if out != text.source.slice(first.start, last.end)
    yield
      Edit(first.start, last.end, out)

  // ----- `/* ... */` and `/** ... */`

  private def blockComments(
    text: CommentSource,
    comments: Vector[Token.Comment],
    maxColumn: Int,
    alignTags: Boolean,
  ): List[Edit] =
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
    val hasBanner   = contents.exists(isBanner)
    val untouched   = oneLineFits || hasBanner
    val paras       = docParas(parse(contents, withTags = true))
    val width       = maxColumn - col - 3

    Option
      .when(!untouched)(render(paras, width, alignTags))
      .filter(_.nonEmpty)
      .map { lines =>
        val indent = " " * col
        val body   = lines.map(line => if line.isEmpty then indent + " *" else indent + " * " + line)
        ("/**" +: body :+ (indent + " */")).mkString("\n")
      }
      .filter(_ != comment.text)
      .map(Edit(comment.start, comment.end, _))

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
      (if content.startsWith(" ") then
        content.drop(1)
      else
        content).stripTrailing
    else
      line
        .drop(math.min(SourceEdits.indent(line), col + 3))
        .stripTrailing

  // No blank lines at the edges, and one blank line before the first tag.
  private def docParas(paras: Vector[Para]): Vector[Para] =
    val trimmed =
      paras
        .dropWhile(_ == Blank)
        .reverse
        .dropWhile(_ == Blank)
        .reverse

    val firstTag = trimmed.indexWhere { case _: Tag => true; case _ => false }
    if firstTag <= 0 then
      trimmed
    else
      val description =
        trimmed
          .take(firstTag)
          .reverse
          .dropWhile(_ == Blank)
          .reverse

      val tags = trimmed.drop(firstTag)

      description ++ Vector(Blank) ++ tags

  private def plainBlock(text: CommentSource, comment: Token.Comment, maxColumn: Int): Option[Edit] =
    val col = text.column(comment.start)
    if !comment.text.contains('\n') then
      Option.when(text.lineWidth(comment.start) > maxColumn) {
        val words = split(comment.text.stripPrefix("/*").stripSuffix("*/")) :+ "*/"
        Edit(comment.start, comment.end, wrap(words, "/* ", "   ", maxColumn - col).mkString("\n" + " " * col))
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

    val contents =
      (if shape.joinsOpener then Vector(opener.trim) else Vector.empty) ++ middle.map(shape.content(_, col))

    val closeInline = tail.trim.nonEmpty
    val paras       = parse(contents, withTags = false)
    val closer      = if closeInline then withCloser(paras) else None
    val rendered    = render(closer.getOrElse(paras), maxColumn - shape.textColumn, alignTags = false)
    val body        = rendered.zipWithIndex.map(shape.line)
    val head        = if shape.joinsOpener then Vector.empty else Vector("/*" + opener.stripTrailing)

    val closed =
      if closer.isDefined then body
      else if closeInline then body.dropRight(1) ++ body.lastOption.map(_ + " */")
      else body :+ closing

    val out       = (head ++ closed).mkString("\n")
    val hasBanner = contents.exists(isBanner)
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
      if gutter then
        withoutGutter(line, col)
      else
        line.drop(math.min(SourceEdits.indent(line), margin)).stripTrailing

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

      val textUnderOpener = opener.trim.nonEmpty && !gutter && margin == col + 3

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

  // ---- Scaladoc and its definition

  private val blankRun = """\A\n((?:[ \t]*\n)+)""".r

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
            Option.when(!separate)(Edit(eol + 1, eol + blankMatch.end, ""))
          }
      }

  // ---- Paragraphs

  private val marker     = """([-*+]|\d{1,3}[.)])"""
  private val listMarker = ("^" + marker + """\s+\S.*""").r
  private val itemMarker = ("^" + marker + "$").r
  private val noteStart  = """^[A-Z]{2,}(\([^)]*\))?:.*""".r
  private val banner     = """^[-=*#_~+/\\]{3,}$""".r
  private val codeEnds   = List("{", "}", "=", "=>", "(")

  private def split(text: String): Vector[String] =
    text
      .trim
      .split("\\s+")
      .toVector
      .filter(_.nonEmpty)

  private def isBanner(line: String): Boolean =
    banner.matches(line.trim)

  private def parse(contents: Vector[String], withTags: Boolean): Vector[Para] =
    val baseLead =
      contents
        .filter(_.trim.nonEmpty)
        .map(SourceEdits.indent)
        .minOption
        .getOrElse(0)

    contents
      .foldLeft((Vector.empty[Para], false)) { case ((paras, fenced), line) =>
        val trimmed = line.trim
        if fenced then
          (paras :+ Verbatim(line), !closesFence(trimmed))
        else if trimmed.isEmpty then
          (paras :+ Blank, false)
        else if opensFence(trimmed) then
          (paras :+ Verbatim(line), !closesOnSameLine(trimmed))
        else
          (classify(paras, line, trimmed, baseLead, withTags), false)
      }
      ._1

  private def opensFence(line: String): Boolean =
    line.startsWith("```") || line.startsWith("{{{") || line.startsWith("<pre")

  private def closesFence(line: String): Boolean =
    line.startsWith("```") || line.contains("}}}") || line.contains("</pre")

  private def closesOnSameLine(line: String): Boolean =
    val closesBraces   = line.startsWith("{{{") && line.contains("}}}")
    val closesHtml     = line.startsWith("<pre") && line.contains("</pre")
    val closesBacktick = line.startsWith("```") && line.drop(3).contains("```")

    closesBraces || closesHtml || closesBacktick

  // A banner, a table row, a heading, or a line aligned with spaces inside it (columns, decorations): reflowing would
  // lose its layout.
  private def isVerbatimLine(line: String): Boolean =
    val tableOrHeading = line.startsWith("|") || line.startsWith("#")
    val equalsHeading  = line.length > 1 && line.startsWith("=") && line.endsWith("=")

    isBanner(line) || tableOrHeading || equalsHeading || line.contains("  ")

  // A line ending or starting like code, or with an assignment (`dt = 1s`).
  private def codeLike(line: String): Boolean =
    val endsLikeCode = codeEnds.exists(line.endsWith) || line.startsWith("}") || line.startsWith(")")

    endsLikeCode || line.contains(" = ")

  private def classify(
    paras: Vector[Para],
    line: String,
    trimmed: String,
    baseLead: Int,
    withTags: Boolean,
  ): Vector[Para] =
    val words    = split(trimmed)
    val inTag    = paras.lastOption.exists { case _: Tag => true; case _ => false }
    val itemMark = listMarker.findPrefixMatchOf(trimmed).map(_.group(1))

    if withTags && trimmed.startsWith("@") then
      paras :+ tag(words)
    else if inTag && !isBanner(trimmed) && itemMark.isEmpty then
      paras.dropRight(1) ++ paras.lastOption.collect { case Tag(h, ws) => Tag(h, ws ++ words) }
    else if isVerbatimLine(trimmed) || trimmed.startsWith("@") then
      paras :+ Verbatim(line)
    else
      itemMark match {
        case Some(m)                            => paras :+ Item(SourceEdits.indent(line), m, words.drop(1))
        case None if codeLike(trimmed)          => paras :+ Verbatim(line)
        case None if noteStart.matches(trimmed) => paras :+ Prose(SourceEdits.indent(line), words)
        case None                               => continueText(paras, line, words, baseLead)
      }

  // A line of text: it continues the paragraph or the list item above it, or else it is code when indented past the
  // text, or a new paragraph.
  private def continueText(paras: Vector[Para], line: String, words: Vector[String], baseLead: Int): Vector[Para] =
    val lead = SourceEdits.indent(line)
    paras.lastOption match {
      case Some(Prose(pl, ws)) if lead == pl  => paras.dropRight(1) :+ Prose(pl, ws ++ words)
      case Some(Item(il, m, ws)) if lead > il => paras.dropRight(1) :+ Item(il, m, ws ++ words)
      case _ if lead > baseLead               => paras :+ Verbatim(line)
      case _                                  => paras :+ Prose(lead, words)
    }

  // The tags that take an argument.
  private val withArgument =
    Set("@param", "@tparam", "@throws", "@define")

  // A tag with its head: `@param name`, `@tparam name` and `@throws Type` take their argument, the other tags only
  // their name.
  private def tag(words: Vector[String]): Para =
    words match {
      case name +: argument +: rest if withArgument.contains(name) => Tag(name + " " + argument, rest)
      case name +: rest                                            => Tag(name, rest)
      case _                                                       => Tag("@", Vector.empty)
    }

  private def render(paras: Vector[Para], width: Int, alignTags: Boolean): Vector[String] =
    val tagWidth =
      if alignTags then
        paras
          .collect { case Tag(h, ws) if ws.nonEmpty => h.length }
          .maxOption
          .getOrElse(0)
      else
        0

    paras.flatMap {
      case Blank =>
        Vector("")
      case Verbatim(line) =>
        Vector(line.stripTrailing)
      case Prose(l, ws) =>
        wrap(ws, " " * l, " " * l, width)
      case Item(l, m, ws) =>
        wrap(ws, " " * l + m + " ", " " * (l + m.length + 1), width)
      case Tag(h, ws) if ws.isEmpty =>
        Vector(h)
      case Tag(h, ws) =>
        val column = math.max(tagWidth, h.length)
        wrap(ws, h.padTo(column, ' ') + " ", " " * (column + 1), width)
    }

  // The words filling lines of `width`, the first after `first` and the others after `cont`.
  private def wrap(words: Vector[String], first: String, cont: String, width: Int): Vector[String] =
    val lines =
      unbreakableUnits(words).foldLeft(Vector.empty[String]) { (acc, unit) =>
        acc.lastOption match {
          case None                                                 => Vector(first + unit)
          case Some(last) if last.length + 1 + unit.length <= width => acc.dropRight(1) :+ (last + " " + unit)
          case Some(_)                                              => acc :+ (cont + unit)
        }
      }

    if lines.isEmpty then Vector(first.stripTrailing)
    else lines

  // The words grouped so that no line starts with a word that would make it a list item, a tag or code, nor ends with
  // one that would make it look like code.
  private def unbreakableUnits(words: Vector[String]): Vector[String] =
    words.foldLeft(Vector.empty[String]) { (acc, word) =>
      acc.lastOption match {
        case Some(prev) if badStart(word) || codeEnds.exists(prev.endsWith) => acc.dropRight(1) :+ (prev + " " + word)
        case _                                                              => acc :+ word
      }
    }

  private def badStart(word: String): Boolean =
    val looksStructured = itemMarker.matches(word) || noteStart.matches(word)
    val looksReserved   = List("@", "|", "#", "=", "}", ")", "```", "{{{", "<pre").exists(word.startsWith)
    looksStructured || looksReserved || isBanner(word)

}
