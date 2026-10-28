package com.colofabrix.scala.dixtra

import cats.data.{Validated, ValidatedNel}
import cats.effect.*
import cats.syntax.all.*
import com.colofabrix.scala.declinio.IODeclineApp
import com.colofabrix.scala.dixtra.cli.*
import com.monovore.decline.Opts
import java.nio.file.Path

/**
 * Command-line entry point for the dixtra formatter.
 *
 * Formats one or more Scala files and/or whole directories in place, or verifies them without writing when `--check` is
 * supplied. Argument parsing is provided by decline via the declinio Cats Effect integration.
 */
object Main extends IODeclineApp[CliOptions] {

  override def name: String =
    "dixtra"

  override def header: String =
    "Format Scala 3 sources with the All-or-Nothing style."

  override def version: String =
    BuildInfo.version

  override def options: Opts[CliOptions] =
    CliParser.options

  override def runWithConfig(cliOptions: CliOptions): IO[ExitCode] =
    program(cliOptions, CliReport.Printer.console)

  /**
   * Format or check the paths of `cliOptions`, reporting each file with `printer`, and return the exit code.
   */
  def program(cliOptions: CliOptions, printer: CliReport.Printer): IO[ExitCode] =
    val configs  = cliOptions.paths.map(path => path -> cliOptions.config.getOrElse(ConfigCli.discover(path)))
    val warnings = configs.flatMap((_, loaded) => loaded.toList.flatMap(_.warnings)).distinct
    warnings.traverse_(warning => printer.err(s"warning: $warning")) >>
    configs
      .flatTraverse((path, loaded) => formatPath(cliOptions, path, loaded))
      .flatMap(results => CliReport.emit(results, cliOptions.check, cliOptions.quiet, printer))

  // Each path is formatted with the configuration given on the command line, or else with the nearest `.dixtra.conf`
  // above it; with `--changed-since`, only its files changed in git are.
  private def formatPath(
    cliOptions: CliOptions,
    path: Path,
    loaded: ValidatedNel[String, LoadedConfig],
  ): IO[List[FileResult]] =
    loaded match {
      case Validated.Invalid(errors) =>
        IO.pure(List(FileResult.Failed(path, errors.toList.mkString("; "))))
      case Validated.Valid(config) =>
        val runner = FormatRunner(config, cliOptions.maxColumn, cliOptions.check)
        selection(cliOptions, path).flatMap {
          case Left(error)   => IO.pure(List(FileResult.Failed(path, error)))
          case Right(select) => runner.run(List(path), select)
        }
    }

  // The files of `path` to process: all of them, or with `--changed-since` the ones changed in git.
  private def selection(cliOptions: CliOptions, path: Path): IO[Either[String, Path => Boolean]] =
    cliOptions.changedSince match {
      case None      => IO.pure(Right(_ => true))
      case Some(ref) => GitChanges.selector(ref, path)
    }

}
