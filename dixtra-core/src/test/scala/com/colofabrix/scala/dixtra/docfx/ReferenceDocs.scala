package com.colofabrix.scala.dixtra.docfx

import java.nio.file.{ Files, Path }
import scala.util.matching.Regex

/**
 * The shape of a settings-reference page.
 *
 * A page holds one `##` section per setting, and each section one `###` subsection per value. Every value subsection
 * carries one Scala code block: the formatted output followed by the `// With settings:` trailer that names the
 * configuration it was produced with.
 */
object ReferenceDocs {

  final case class Subsection(
    key: String,
    value: String,
    outputFrom: Int,
    outputTo: Int,
    output: List[String],
    trailer: List[(String, String)],
    sameAs: Option[String],
  )

  final case class Section(
    key: String,
    default: Option[String],
    before: Option[String],
    subsections: List[Subsection],
  )

  final case class Page(
    path: Path,
    name: String,
    lines: List[String],
    sections: List[Section],
  )

  private val sectionHeading: Regex =
    """^## `([^`]+)`$""".r

  private val subsectionHeading: Regex =
    """^### `([^`]+) = ([^`]+)`$""".r

  private val defaultLinePattern: Regex =
    """^\*\*Default:\*\* `([^`]+)`\.$""".r

  private val trailerMarker =
    "// With settings:"

  private val trailerPrefix =
    "//"

  private val sameOutputPattern: Regex =
    """^<!-- same-output-as: ([^ ]+) -->$""".r

  def loadPages(dir: Path): List[Page] =
    if !Files.isDirectory(dir) then
      Nil
    else
      val files =
        Files
          .list(dir)
          .toArray
          .toList
          .map(_.asInstanceOf[Path])
          .filter(_.toString.endsWith(".md"))

      files.sortBy(_.toString).map(parsePage)

  def parsePage(path: Path): Page =
    val lines =
      Files
        .readAllLines(path)
        .toArray
        .toList
        .map(_.asInstanceOf[String])

    val sectionStarts =
      lines
        .zipWithIndex
        .flatMap { case (line, index) =>
          sectionHeading.findFirstMatchIn(line).map(matched => (index, matched.group(1)))
        }

    val subsectionStarts =
      lines
        .zipWithIndex
        .flatMap { case (line, index) =>
          subsectionHeading.findFirstMatchIn(line).map(matched => (index, matched.group(1), matched.group(2)))
        }

    val sections =
      sectionStarts.map { case (start, key) =>
        parseSection(lines, sectionStarts, subsectionStarts, start, key)
      }

    Page(path, path.getFileName.toString, lines, sections)

  private def parseSection(
    lines: List[String],
    sectionStarts: List[(Int, String)],
    subsectionStarts: List[(Int, String, String)],
    start: Int,
    key: String,
  ): Section =
    val nextSectionStart =
      sectionStarts
        .map(_._1)
        .filter(_ > start)
        .minOption
        .getOrElse(lines.length)

    val ownSubsections = subsectionStarts.filter { case (index, _, _) => index > start && index < nextSectionStart }

    val bodyEnd =
      ownSubsections
        .headOption
        .map(_._1)
        .getOrElse(nextSectionStart)

    val body    = lines.slice(start + 1, bodyEnd)
    val default = body.collectFirst { case defaultLinePattern(value) => value }
    val before  = findFence(body).map { case (open, close) => body.slice(open + 1, close).mkString("\n") }

    val subsections =
      ownSubsections.map { case (subStart, subKey, subValue) =>
        val laterStarts = ownSubsections.map(_._1) ++ List(nextSectionStart)

        val subEnd =
          laterStarts
            .filter(_ > subStart)
            .minOption
            .getOrElse(lines.length)

        parseSubsection(lines, subStart, subEnd, subKey, subValue)
      }

    Section(key, default, before, subsections)

  private def parseSubsection(lines: List[String], start: Int, end: Int, key: String, value: String): Subsection =
    val body = lines.slice(start + 1, end)

    findFence(body) match {
      case None =>
        Subsection(key, value, -1, -1, Nil, Nil, None)
      case Some((open, close)) =>
        val block        = body.slice(open + 1, close)
        val markerIndex  = block.indexWhere(_ == trailerMarker)
        val rawOutput    = if markerIndex < 0 then block else block.take(markerIndex)
        val trailerLines = if markerIndex < 0 then Nil else block.drop(markerIndex)

        val output =
          rawOutput
            .reverse
            .dropWhile(_.isBlank)
            .reverse

        val entries =
          trailerLines
            .drop(1)
            .map(_.stripPrefix(trailerPrefix).trim)
            .filter(_.nonEmpty)
            .flatMap(splitAssignment)

        val outputFrom = start + 1 + open + 1
        
        val sameAs =
          body
            .take(open)
            .flatMap { case sameOutputPattern(other) => Some(other) case _ => None }
            .headOption

        Subsection(key, value, outputFrom, outputFrom + rawOutput.size, output, entries, sameAs)
    }

  private def findFence(lines: List[String]): Option[(Int, Int)] =
    val open = lines.indexWhere(_.trim == "```scala")
    if open < 0 then
      None
    else
      val close = lines.indexWhere(_.trim == "```", open + 1)
      if close < 0 then None else Some((open, close))

  private def splitAssignment(line: String): Option[(String, String)] =
    line.split(" = ", 2) match {
      case Array(key, value) if key.nonEmpty && value.nonEmpty => Some((key.trim, value.trim))
      case _                                                   => None
    }

}
