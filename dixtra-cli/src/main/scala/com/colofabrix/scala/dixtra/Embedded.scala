package com.colofabrix.scala.dixtra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.colofabrix.scala.dixtra.cli.*
import com.monovore.decline.Command
import java.util.function.Consumer

/**
 * The command line of [[Main]] for tools that run dixtra inside their own JVM, like the sbt plugin: it does not exit,
 * and it hands the report lines to the caller. It takes and returns only Java types, so it can be called through
 * reflection from another class loader, with another Scala version.
 */
object Embedded {

  /**
   * Run the command line `args` (as for [[Main]]).
   *
   * @param args The command-line arguments.
   * @param out Receives the line of each processed file.
   * @param err Receives the warnings, the failures and the usage errors.
   * @return The exit code: 0 on success, 1 when a file failed or, in check mode, would change, 2 for invalid arguments.
   */
  def run(args: Array[String], out: Consumer[String], err: Consumer[String]): Int =
    Command(Main.name, Main.header)(CliParser.options).parse(args.toList) match {
      case Left(help) =>
        err.accept(help.toString)
        2
      case Right(cliOptions) =>
        val printer = CliReport.Printer(line => IO(out.accept(line)), line => IO(err.accept(line)))
        Main
          .program(cliOptions, printer)
          .unsafeRunSync()
          .code
    }

}
