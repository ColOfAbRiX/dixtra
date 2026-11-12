package com.colofabrix.scala.dixtra.cli

import cats.data.NonEmptyList
import cats.syntax.all.*
import com.colofabrix.scala.dixtra.cli.config.ConfigCli
import com.monovore.decline.Opts
import java.nio.file.Path

/**
 * Decline option definitions for the dixtra CLI.
 *
 * Accepts one or more file or directory paths as positional arguments, an optional configuration (see
 * [[com.colofabrix.scala.dixtra.cli.config.ConfigCli]]),
 * an optional `--max-column` width (which must be a positive integer) overriding the configured one, a `--check` flag
 * that switches the tool into verification mode, a `--changed-since` git ref that limits it to the files changed since
 * then, and a `--quiet` flag that leaves the unchanged files out of the report.
 */
object CliParser {

  private val paths: Opts[NonEmptyList[Path]] =
    Opts.arguments[Path]("path")

  private val maxColumn: Opts[Option[Int]] =
    Opts
      .option[Int]("max-column", "Maximum line width before a construct breaks.", "w")
      .mapValidated { width =>
        if width > 0 then width.validNel
        else s"--max-column must be a positive integer, got ${width.toString}".invalidNel
      }
      .orNone

  private val check: Opts[Boolean] =
    Opts
      .flag("check", "Report unformatted files without writing changes.", "c")
      .orFalse

  private val changedSince: Opts[Option[String]] =
    Opts
      .option[String](
        "changed-since",
        "Only the files changed since a git ref: committed since, uncommitted or untracked " +
        "(HEAD: only the uncommitted ones).",
        metavar = "ref",
      )
      .orNone

  private val quiet: Opts[Boolean] =
    Opts
      .flag("quiet", "List only the files that change or fail, not the unchanged ones.", "q")
      .orFalse

  /**
   * The combined CLI options.
   */
  val options: Opts[CliOptions] =
    (paths.map(_.toList), ConfigCli.declineOpts, maxColumn, check, changedSince, quiet).mapN(CliOptions.apply)

}
