package com.colofabrix.scala.dixtra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.colofabrix.scala.dixtra.cli.*
import com.colofabrix.scala.dixtra.cli.engine.ResultCache
import com.monovore.decline.Command
import java.nio.file.*
import java.util.function.Consumer

/**
 * The command line of [[Main]] for tools that run dixtra inside their own JVM, like the sbt plugin: it does not exit,
 * and it hands the report lines to the caller. It takes and returns only Java types, so it can be called through
 * reflection from another class loader, with another Scala version.
 */
object Embedded {

  /**
   * Run the command line `args` (as for [[Main]]), remembering the files found formatted where the command line does.
   *
   * @param args The command-line arguments.
   * @param out Receives the line of each processed file.
   * @param err Receives the warnings, the failures and the usage errors.
   * @return The exit code: 0 on success, 1 when a file failed or, in check mode, would change, 2 for invalid arguments.
   */
  def run(args: Array[String], out: Consumer[String], err: Consumer[String]): Int =
    runWith(args, out, err, cliOptions => ResultCache.userFile(cliOptions.paths))

  /**
   * Run the command line `args` (as for [[Main]]), remembering the files found formatted in `cacheFile`, so that the
   * next runs with it do not format them again; with `null`, nothing is remembered.
   *
   * @param args The command-line arguments.
   * @param out Receives the line of each processed file.
   * @param err Receives the warnings, the failures and the usage errors.
   * @param cacheFile The file of the files found formatted, or `null`.
   * @return The exit code: 0 on success, 1 when a file failed or, in check mode, would change, 2 for invalid arguments.
   */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def run(args: Array[String], out: Consumer[String], err: Consumer[String], cacheFile: String): Int =
    runWith(args, out, err, _ => Option(cacheFile).map(Paths.get(_)))

  private def runWith(
    args: Array[String],
    out: Consumer[String],
    err: Consumer[String],
    cacheFile: CliOptions => Option[Path],
  ): Int =
    Command(Main.name, Main.header)(CliParser.options).parse(args.toList) match {
      case Left(help) =>
        err.accept(help.toString)
        2
      case Right(cliOptions) =>
        val printer = CliReport.Printer(line => IO(out.accept(line)), line => IO(err.accept(line)))
        Main
          .program(cliOptions, printer, cacheFile(cliOptions))
          .unsafeRunSync()
          .code
    }

}
