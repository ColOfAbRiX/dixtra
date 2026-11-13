package com.colofabrix.scala.dixtra.cli.engine

import cats.effect.IO
import cats.syntax.all.*
import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.cli.config.LoadedConfig
import com.colofabrix.scala.dixtra.cli.files.*
import com.colofabrix.scala.dixtra.model.*
import java.nio.file.Path

/**
 * Applies the formatter to the Scala sources collected from the CLI paths (see
 * [[com.colofabrix.scala.dixtra.cli.files.SourceFiles]]), keeping the formatter core pure.
 *
 * Files are read and formatted in parallel, after the first one alone (see `prepareAll`); the formatted files are then
 * either rewritten atomically (format mode, see [[com.colofabrix.scala.dixtra.cli.files.AtomicFiles]]) or compared
 * without writing (check mode), one after the other in the order they arrived. Every file is processed in isolation:
 * any error while reading, formatting or writing it becomes a
 * [[com.colofabrix.scala.dixtra.cli.files.FileResult.Failed]] and the remaining files are still processed. A Scala 2
 * file is not formatted and becomes a [[com.colofabrix.scala.dixtra.cli.files.FileResult.Skipped]]. A file that `cache`
 * knows as formatted is not formatted again.
 *
 * @param check When true, run in verification mode and never write to disk.
 * @param format The formatting function of each file, applied to its contents, or why the file cannot be formatted (an
 *               invalid configuration for it).
 * @param paths The files and directories to format inside the directories given (see
 *              [[com.colofabrix.scala.dixtra.cli.files.SourceFiles]]).
 * @param settings The configuration of each file as text, for `cache`, or None for a file that is never cached.
 * @param cache The files that earlier runs found formatted.
 */
final class FormatRunner private (
  check: Boolean,
  format: Path => Either[String, String => Either[FormatError, String]],
  paths: PathsConfig,
  settings: Path => Option[String],
  cache: ResultCache,
) {

  /**
   * Process every input path, returning one result per discovered Scala file in the order the files were discovered.
   * Input paths that cannot be expanded are reported as failures ahead of the per-file results.
   */
  def run(inputs: List[Path]): IO[List[FileResult]] =
    run(inputs, _ => true)

  /**
   * Like [[run]], processing only the discovered files for which `select` holds (e.g. the files changed in git).
   */
  def run(inputs: List[Path], select: Path => Boolean): IO[List[FileResult]] =
    for
      entries          <- SourceFiles.discover(inputs, paths)
      (failures, files) = entries.partitionMap(identity)
      selected          = files.distinct.filter(select)
      completions      <- prepareAll(selected)
      results          <- completions.sequence
    yield
      failures ++ results

  // The first file alone, then the others in parallel. The JVM links each lambda call site the first time it runs, and
  // threads that reach an unlinked site together each spin a class for it: formatting every file at once from cold made
  // the threads queue on the class loader for most of the run. Formatting one file first links the common sites once.
  private def prepareAll(files: List[Path]): IO[List[IO[FileResult]]] =
    files match {
      case first :: rest => prepareFile(first).flatMap(done => rest.parTraverse(prepareFile).map(done :: _))
      case Nil           => IO.pure(Nil)
    }

  // Read and format a file, returning the action that completes it: writing the formatted content, or just reporting
  // the result when there is nothing to write.
  private def prepareFile(path: Path): IO[IO[FileResult]] =
    format(path) match {
      case Left(error) =>
        IO.pure(IO.pure(FileResult.Failed(path, error)))
      case Right(formatter) =>
        AtomicFiles
          .read(path)
          .flatMap { source =>
            cache
              .isFormatted(path, settings(path), source)
              .ifM(IO.pure(IO.pure(FileResult.Unchanged(path))), applyFormat(path, source, formatter))
          }
          .handleError(e => IO.pure(FileResult.failed(path, e)))
    }

  private def applyFormat(
    path: Path,
    source: String,
    formatter: String => Either[FormatError, String],
  ): IO[IO[FileResult]] =
    formatSource(source, formatter).map {
      case Left(_: Scala2Error) =>
        IO.pure(FileResult.Skipped(path, "scala 2"))
      case Left(error) =>
        IO.pure(FileResult.failed(path, error))
      case Right(formatted) if formatted == source =>
        cache.formatted(path, settings(path), source).as(FileResult.Unchanged(path))
      case Right(_) if check =>
        IO.pure(FileResult.WouldFormat(path))
      case Right(formatted) =>
        AtomicFiles
          .write(path, formatted)
          .as(FileResult.Formatted(path))
          .handleError(e => FileResult.failed(path, e))
    }

  // A stack overflow is fatal for cats-effect, so it is caught here to keep a single pathological file from aborting
  // the whole run.
  private def formatSource(
    source: String,
    formatter: String => Either[FormatError, String],
  ): IO[Either[FormatError, String]] =
    IO {
      try {
        formatter(source)
      } catch {
        case _: StackOverflowError =>
          Left(UnsafeOutputError("the formatter ran out of stack space on this file"))
      }
    }

}

object FormatRunner {

  /**
   * A runner that formats files with [[Dixtra.format]] in the dialect of each file (see [[SourceDialect]]), selecting
   * them with the configured paths.
   *
   * @param config The formatting configuration derived from the CLI options.
   * @param check When true, run in verification mode and never write to disk.
   */
  def apply(config: Config, check: Boolean): FormatRunner =
    new FormatRunner(check, path => Right(dixtra(config, path)), config.paths, _ => None, ResultCache.disabled)

  /**
   * A runner that formats each file with [[Dixtra.format]] in its dialect, with its configuration of `loaded` (see
   * [[com.colofabrix.scala.dixtra.cli.config.LoadedConfig.configFor]]), selecting the files with the paths of `loaded`.
   * A file whose configuration is invalid fails.
   *
   * @param loaded The configuration, with its overrides for some files.
   * @param maxColumn The maximum width given on the command line, over the one of every configuration.
   * @param check When true, run in verification mode and never write to disk.
   * @param cache The files that earlier runs found formatted.
   */
  def apply(loaded: LoadedConfig, maxColumn: Option[Int], check: Boolean, cache: ResultCache): FormatRunner =
    def effective(path: Path): Either[String, Config] =
      loaded
        .configFor(path)
        .toEither
        .bimap(
          _.toList.mkString("; "),
          config => maxColumn.fold(config)(column => config.copy(maxColumn = column)),
        )

    new FormatRunner(
      check,
      path => effective(path).map(dixtra(_, path)),
      loaded.base.paths,
      path => effective(path).toOption.map(_.toString),
      cache,
    )

  /**
   * Like the other `apply`, without a cache.
   */
  def apply(loaded: LoadedConfig, maxColumn: Option[Int], check: Boolean): FormatRunner =
    apply(loaded, maxColumn, check, ResultCache.disabled)

  // Dixtra with `config`, in the dialect of `path`.
  private def dixtra(config: Config, path: Path): String => Either[FormatError, String] =
    source => Dixtra.format(source, config, SourceDialect.of(path.toString))

  /**
   * A runner that formats every file found with `format`.
   *
   * @param check When true, run in verification mode and never write to disk.
   * @param format The formatting function applied to the contents of each file.
   */
  def withFormat(check: Boolean, format: String => Either[FormatError, String]): FormatRunner =
    new FormatRunner(check, _ => Right(format), PathsConfig.everything, _ => None, ResultCache.disabled)

  /**
   * A runner that formats every file found with `format`, except the ones that `cache` knows as formatted with the
   * configuration `settings`.
   *
   * @param check When true, run in verification mode and never write to disk.
   * @param format The formatting function applied to the contents of each file.
   * @param cache The files that earlier runs found formatted.
   * @param settings The configuration of every file as text.
   */
  def withFormat(
    check: Boolean,
    format: String => Either[FormatError, String],
    cache: ResultCache,
    settings: String,
  ): FormatRunner =
    new FormatRunner(check, _ => Right(format), PathsConfig.everything, _ => Some(settings), cache)

  /**
   * A runner that formats the files selected by `paths` with `format`.
   *
   * @param check When true, run in verification mode and never write to disk.
   * @param format The formatting function applied to the path and the contents of each file.
   * @param paths The files to format inside the directories given (see
   *              [[com.colofabrix.scala.dixtra.cli.files.SourceFiles]]).
   */
  def withFormat(
    check: Boolean,
    format: (Path, String) => Either[FormatError, String],
    paths: PathsConfig,
  ): FormatRunner =
    new FormatRunner(check, path => Right(format(path, _)), paths, _ => None, ResultCache.disabled)

}
