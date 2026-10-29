package com.colofabrix.scala.dixtra.cli

import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import java.nio.file.Path

/**
 * Renders per-file results to the console and derives the process exit code.
 *
 * In format mode the run fails only when a file could not be processed. In check mode it additionally fails when any
 * file would change, so the CLI is usable as a CI gate. A skipped file (Scala 2) is reported as a warning and fails
 * neither. A file under the working directory, the root of the project, is printed relative to it. A last line sums the
 * results up.
 */
object CliReport {

  /**
   * Where the report lines go.
   *
   * @param out Receives the line of each processed file.
   * @param err Receives the warnings and the failures.
   */
  final case class Printer(out: String => IO[Unit], err: String => IO[Unit])

  object Printer {

    /**
     * The standard output and the standard error.
     */
    val console: Printer =
      Printer(line => Console[IO].println(line), line => Console[IO].errorln(line))

  }

  /**
   * Print one line per result with `printer`, but for the unchanged files when `quiet`, then the summary, and return
   * the appropriate exit code.
   */
  def emit(results: List[FileResult], check: Boolean, quiet: Boolean, printer: Printer): IO[ExitCode] =
    val listed = if quiet then results.filterNot(outcome(_) == "unchanged") else results
    listed.traverse_(printResult(printer, _)) *> printer.out(summary(results)) *> IO.pure(exitCode(results, check))

  /**
   * The summary of a run: how many files it saw, and how many of them ended in each way, e.g. `12 files: 3 formatted, 9
   * unchanged`.
   */
  def summary(results: List[FileResult]): String =
    val outcomes = results.map(outcome)
    val counts =
      List("formatted", "would format", "unchanged", "skipped", "failed")
        .map(o => o -> outcomes.count(_ == o))
        .collect { case (o, n) if n > 0 => s"${n.toString} $o" }
    val files = if results.sizeIs == 1 then "1 file" else s"${results.size.toString} files"
    if counts.isEmpty then files else s"$files: ${counts.mkString(", ")}"

  // How a file ended, in the words of the summary.
  private def outcome(result: FileResult): String =
    result match {
      case _: FileResult.Formatted   => "formatted"
      case _: FileResult.WouldFormat => "would format"
      case _: FileResult.Unchanged   => "unchanged"
      case _: FileResult.Skipped     => "skipped"
      case _: FileResult.Failed      => "failed"
    }

  // The sbt plugin logs a line by its prefix (`unchanged:` at debug, `skipped` as a warning, and the summary prefixed
  // with the project it runs for, see `DixtraPlugin.invoke`): they must stay in step.
  private def printResult(printer: Printer, result: FileResult): IO[Unit] =
    result match {
      case FileResult.Formatted(path) =>
        printer.out(s"formatted: ${shown(path)}")
      case FileResult.WouldFormat(path) =>
        printer.out(s"would format: ${shown(path)}")
      case FileResult.Unchanged(path) =>
        printer.out(s"unchanged: ${shown(path)}")
      case FileResult.Skipped(path, reason) =>
        printer.err(s"skipped ($reason): ${shown(path)}")
      case FileResult.Failed(path, message) =>
        printer.err(s"failed: ${shown(path)}: $message")
    }

  // A path relative to the working directory when it is under it, absolute otherwise.
  private def shown(path: Path): String =
    val root     = Path.of("").toAbsolutePath
    val absolute = path.toAbsolutePath.normalize
    if absolute.startsWith(root) && absolute != root then root.relativize(absolute).toString else absolute.toString

  // The exit code of a run: an error when a file failed, or in check mode when a file would change.
  private def exitCode(results: List[FileResult], check: Boolean): ExitCode =
    val hasFailure = results.exists { case _: FileResult.Failed => true; case _ => false }
    val wouldChange =
      check && results.exists { case _: FileResult.WouldFormat => true; case _ => false }
    if hasFailure || wouldChange then ExitCode.Error else ExitCode.Success

}
