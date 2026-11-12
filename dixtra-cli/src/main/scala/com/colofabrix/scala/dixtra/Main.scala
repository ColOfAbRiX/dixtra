package com.colofabrix.scala.dixtra

import cats.data.*
import cats.effect.*
import cats.syntax.all.*
import com.colofabrix.scala.declinio.IODeclineApp
import com.colofabrix.scala.dixtra.cli.*
import com.colofabrix.scala.dixtra.cli.config.*
import com.colofabrix.scala.dixtra.cli.engine.{FormatRunner, ResultCache}
import com.colofabrix.scala.dixtra.cli.files.*
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
    "Opinionated formatter for Scala 3."

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
    val configs =
      cliOptions
        .paths
        .map { path =>
          path -> cliOptions.config.getOrElse(ConfigCli.discover(path))
        }

    val warnings =
      configs
        .flatMap((_, loaded) => loaded.toList.flatMap(_.warnings))
        .distinct

    for
      _       <- warnings.traverse_(warning => printer.err(s"warning: $warning"))
      cache   <- cliOptions.cacheFile.fold(IO.pure(ResultCache.disabled))(file => ResultCache.load(file))
      results <- configs.flatTraverse((path, loaded) => formatPath(cliOptions, path, loaded, cache))
      _       <- cliOptions.cacheFile.traverse_(file => saveCache(cache, file, printer))
      code    <- CliReport.emit(results, cliOptions.check, cliOptions.quiet, printer)
    yield
      code

  // A cache that cannot be written only costs the next run its time: it is a warning.
  private def saveCache(cache: ResultCache, file: Path, printer: CliReport.Printer): IO[Unit] =
    cache
      .save(file)
      .handleErrorWith(e => printer.err(s"warning: the cache file ${file.toString} cannot be written: ${e.getMessage}"))

  // Each path is formatted with the configuration given on the command line, or else with the nearest `.dixtra.conf`
  // above it; with `--changed-since`, only its files changed in git are.
  private def formatPath(
    cliOptions: CliOptions,
    path: Path,
    loaded: ValidatedNel[String, LoadedConfig],
    cache: ResultCache,
  ): IO[List[FileResult]] =
    loaded match {
      case Validated.Invalid(errors) =>
        IO.pure(List(FileResult.Failed(path, errors.toList.mkString("; "))))
      case Validated.Valid(config) =>
        selection(cliOptions, path).flatMap {
          case Left(error)   => 
            IO.pure(List(FileResult.Failed(path, error)))
          case Right(select) => 
            FormatRunner(config, cliOptions.maxColumn, cliOptions.check, cache)
            .run(List(path), select)
        }
    }

  // The files of `path` to process: all of them, or with `--changed-since` the ones changed in git.
  private def selection(cliOptions: CliOptions, path: Path): IO[Either[String, Path => Boolean]] =
    cliOptions.changedSince match {
      case None      => IO.pure(Right(_ => true))
      case Some(ref) => GitChanges.selector(ref, path)
    }

}
