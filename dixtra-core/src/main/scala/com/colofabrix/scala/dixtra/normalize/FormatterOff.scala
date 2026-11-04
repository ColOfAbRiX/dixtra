package com.colofabrix.scala.dixtra.normalize

import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * The regions of a file that dixtra leaves as the author wrote them: from a comment `// dixtra: off` to the next
 * comment `// dixtra: on`, or to the end of the file. A marker is a line or a block comment holding only the marker, in
 * any letter case and with any spaces: `//DIXTRA:OFF` and `/* dixtra : on */` are markers too. An `off` inside a
 * region, or an `on` outside one, does nothing.
 *
 * The file is formatted as a whole, then the text between the markers of each region is put back as it was written. The
 * code around a region is formatted, so a change around it that cannot keep the region as written (for example the
 * indentation of a whole package body) makes the output unsafe, and the file is left unchanged.
 */
object FormatterOff {

  private val Marker =
    """(?i)(?://|/\*)\s*dixtra\s*:\s*(off|on)\s*(?:\*/)?""".r

  /**
   * `formatted`, the formatted text of `original`, with the regions of `original` between its markers put back as
   * written; Left when the two texts do not have the same regions.
   */
  def restore(original: String, formatted: String, dialect: Dialect): Either[String, String] =
    regions(original, dialect) match {
      case Some(Nil) =>
        Right(formatted)
      case written =>
        (written, regions(formatted, dialect)) match {
          case (Some(before), Some(after)) if before.sizeCompare(after) == 0 =>
            val (text, last) =
              before
                .zip(after)
                .foldLeft((new StringBuilder, 0)) { case ((acc, from), ((start, end), (fStart, fEnd))) =>
                  (acc ++= formatted.substring(from, fStart) ++= original.substring(start, end), fEnd)
                }

            Right((text ++= formatted.substring(last)).toString)
          case _ =>
            Left("the regions between `dixtra: off` and `dixtra: on` could not be kept as written")
        }
    }

  // The regions of `source`: from the end of each `off` marker to the start of the `on` marker after it, or to the end
  // of the source. None when the source cannot be tokenised.
  private def regions(source: String, dialect: Dialect): Option[List[(Int, Int)]] =
    tokens(source, dialect).map { all =>
      val markers =
        all
          .collect { case comment: Token.Comment => comment }
          .flatMap { comment =>
            comment.text.trim match {
              case Marker(state) => Some((state.toLowerCase == "off", comment))
              case _             => None
            }
          }

      val (closed, open) =
        markers.foldLeft((List.empty[(Int, Int)], Option.empty[Int])) {
          case ((acc, None), (true, comment))         => (acc, Some(comment.end))
          case ((acc, Some(start)), (false, comment)) => (acc :+ (start -> comment.start), None)
          case (state, _)                             => state
        }

      closed ++ open.map(_ -> source.length)
    }

  private def tokens(source: String, dialect: Dialect): Option[Vector[Token]] =
    given Dialect = dialect
    try {
      source
        .tokenize
        .toOption
        .map(_.toVector)
    } catch {
      case NonFatal(_) => None
    }

}
