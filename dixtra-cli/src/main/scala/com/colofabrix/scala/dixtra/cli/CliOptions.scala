package com.colofabrix.scala.dixtra.cli

import cats.data.ValidatedNel
import com.colofabrix.scala.dixtra.cli.config.LoadedConfig
import java.nio.file.Path

/**
 * Parsed command-line options for the dixtra formatter CLI.
 *
 * @param paths The files and/or directories to format. Directories are searched recursively for Scala sources.
 * @param config The configuration given on the command line, if any, or the errors that make it invalid; otherwise each
 *               path uses the nearest `.dixtra.conf` or the defaults.
 * @param maxColumn The maximum line width, overriding the configuration.
 * @param check When true, do not modify files: only report whether each file is already formatted, exiting non-zero if
 *              any would change.
 * @param changedSince A git ref: when given, only the files changed since it (see
 *                     [[com.colofabrix.scala.dixtra.cli.files.GitChanges]]) are processed.
 * @param quiet When true, the unchanged files are not listed.
 */
final case class CliOptions(
  paths: List[Path],
  config: Option[ValidatedNel[String, LoadedConfig]],
  maxColumn: Option[Int],
  check: Boolean,
  changedSince: Option[String],
  quiet: Boolean,
)
