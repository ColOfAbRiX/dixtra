package com.colofabrix.scala.dixtra.model

/**
 * The files and directories to format.
 *
 * Patterns are globs matched against paths relative to the directory being formatted, with `/` as separator: `*`
 * matches within a path segment, `**` across segments, `?` one character.
 *
 * @param included A file is formatted only if its path matches one of these.
 * @param excluded A file is not formatted if its path, or the path of a directory above it, matches one of these.
 */
final case class PathsConfig(included: List[String], excluded: List[String])

object PathsConfig {

  /**
   * Every file.
   */
  val everything: PathsConfig =
    PathsConfig(
      included = List("**"),
      excluded = Nil,
    )

}
