package com.colofabrix.scala.dixtra.normalize.comments

import com.colofabrix.scala.dixtra.normalize.SourceEdits

/**
 * The text of a comment as paragraphs, and their layout to a width.
 *
 * Blank lines, tags, list items, headings and tables start their own paragraph; code (fences, indented lines and lines
 * that look like code) and words longer than the line are never changed. A line never ends with a word that would make
 * it look like code, nor starts with one that would make it a list item or a tag, so the result is stable on a second
 * run.
 */
private[normalize] object Paragraphs {

  /**
   * The lines of a comment, grouped by meaning.
   */
  enum Para {
    case Blank
    case Verbatim(line: String)
    case Prose(lead: Int, words: Vector[String])
    case Item(lead: Int, marker: String, words: Vector[String])
    case Tag(head: String, words: Vector[String])
  }
  import Para.*

  private val marker     = """([-*+]|\d{1,3}[.)])"""
  private val listMarker = ("^" + marker + """\s+\S.*""").r
  private val itemMarker = ("^" + marker + "$").r
  private val noteStart  = """^[A-Z]{2,}(\([^)]*\))?:.*""".r
  private val banner     = """^[-=*#_~+/\\]{3,}$""".r
  private val codeEnds   = List("{", "}", "=", "=>", "(")

  // The tags that take an argument.
  private val withArgument =
    Set("@param", "@tparam", "@throws", "@define")

  /**
   * The words of `text`, split at whitespace.
   */
  def split(text: String): Vector[String] =
    text
      .trim
      .split("\\s+")
      .toVector
      .filter(_.nonEmpty)

  /**
   * Whether `line` is a banner, a run of decoration characters, which keeps its comment as written.
   */
  def isBanner(line: String): Boolean =
    banner.matches(line.trim)

  /**
   * The paragraphs of the text lines of a comment; `withTags` reads the lines starting with `@` as scaladoc tags.
   */
  def parse(contents: Vector[String], withTags: Boolean): Vector[Para] =
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

  /**
   * The paragraphs as lines of `width`, the descriptions of the tags on one column with `alignTags`.
   */
  def render(paras: Vector[Para], width: Int, alignTags: Boolean): Vector[String] =
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

  /**
   * The words filling lines of `width`, the first after `first` and the others after `cont`.
   */
  def wrap(words: Vector[String], first: String, cont: String, width: Int): Vector[String] =
    val lines =
      unbreakableUnits(words).foldLeft(Vector.empty[String]) { (acc, unit) =>
        acc.lastOption match {
          case None                                                 => Vector(first + unit)
          case Some(last) if last.length + 1 + unit.length <= width => acc.dropRight(1) :+ (last + " " + unit)
          case Some(_)                                              => acc :+ (cont + unit)
        }
      }

    if lines.isEmpty then
      Vector(first.stripTrailing)
    else
      lines

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

  // A tag with its head: `@param name`, `@tparam name` and `@throws Type` take their argument, the other tags only
  // their name.
  private def tag(words: Vector[String]): Para =
    words match {
      case name +: argument +: rest if withArgument.contains(name) => Tag(name + " " + argument, rest)
      case name +: rest                                            => Tag(name, rest)
      case _                                                       => Tag("@", Vector.empty)
    }

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
