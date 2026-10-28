import sbt.*
import sbt.Keys.*
import scala.sys.process.*

/**
 * Builds a project as a GraalVM native executable with `native-image`.
 *
 * The GraalVM installation is the one `GRAALVM_HOME` points to. On Windows `native-image` needs the Visual Studio C++
 * build tools.
 *
 * The reachability metadata (reflection, resources) is read from `META-INF/native-image/<organization>/<name>` in the
 * resources of the project. `nativeImageMetadata` writes it: it runs the main class on the JVM of GraalVM with its
 * tracing agent, once for each of `nativeImageRuns`, on a scratch copy of the sources of the project.
 */
object NativeImage {

  val nativeImage =
    taskKey[File]("Build the project as a GraalVM native executable, in target/native")
  val nativeImageMetadata =
    taskKey[File]("Write the reachability metadata of native-image by tracing nativeImageRuns")
  val nativeImageRuns =
    settingKey[File => Seq[Seq[String]]]("The arguments of each traced run, given a scratch directory")

  /**
   * The tasks building `mainClass` into an executable named `executable`.
   */
  def settings(mainClass: String, executable: String): Seq[Setting[?]] =
    Seq(
      nativeImage := Def.uncached {
        val log       = streams.value.log
        val classpath = runtimeClasspath(fileConverter.value, (Compile / fullClasspath).value)
        val directory = target.value / "native"
        val output    = directory / executable

        // The arguments go through a file: on Windows the classpath is too long for a command line
        val arguments =
          List(
            "-march=compatibility",
            // The lazy vals of the Scala runtime use sun.misc.Unsafe: without this, every run warns about it
            "-J--sun-misc-unsafe-memory-access=allow",
            "-H:+ReportExceptionStackTraces",
            "-cp",
            classpath.map(path).mkString(java.io.File.pathSeparator),
            "-o",
            path(output),
            mainClass,
          )

        IO.createDirectory(directory)

        val argumentFile = directory / "native-image.args"
        IO.writeLines(argumentFile, arguments.map(argument => "\"" + argument + "\""))

        log.info(s"building ${path(output)}")
        run(log, List(path(tool(if windows then "native-image.cmd" else "native-image")), "@" + path(argumentFile)))

        if windows then
          directory / (executable + ".exe")
        else
          output
      },
      nativeImageMetadata := Def.uncached {
        val log       = streams.value.log
        val classpath = runtimeClasspath(fileConverter.value, (Compile / fullClasspath).value)
        val scratch   = target.value / "native-image-runs"
        val metadata =
          (Compile / resourceDirectory).value / "META-INF" / "native-image" / organization.value / name.value

        IO.delete(scratch)
        IO.createDirectory(scratch)
        IO.delete(metadata)
        IO.createDirectory(metadata)

        nativeImageRuns
          .value(scratch)
          .foreach { arguments =>
            log.info(s"tracing ${arguments.mkString(" ")}")
            // A run that reports an error is traced too: its exit code is not a failure
            val command =
              List(path(tool("java")), s"-agentlib:native-image-agent=config-merge-dir=${path(metadata)}") ++
              List("-cp", classpath.map(path).mkString(java.io.File.pathSeparator), mainClass) ++ arguments

            Process(command, scratch).!(ProcessLogger(_ => (), _ => ()))
          }

        metadata
      },
    )

  private def runtimeClasspath(converter: xsbti.FileConverter, classpath: Classpath): Seq[File] =
    classpath.map(entry => converter.toPath(entry.data).toFile)

  private def windows: Boolean =
    sys
      .props("os.name")
      .toLowerCase
      .contains("windows")

  private def tool(name: String): File =
    val graal = sys.env.getOrElse("GRAALVM_HOME", sys.error("GRAALVM_HOME must point to a GraalVM installation"))
    file(graal) / "bin" / name

  private def run(log: Logger, command: Seq[String]): Unit =
    val exit = Process(command).!(ProcessLogger(log.info(_), log.warn(_)))
    if exit != 0 then
      sys.error(s"${command.headOption.getOrElse("")} failed with exit code $exit")

  // Forward slashes: an argument file reads a backslash as an escape
  private def path(file: File): String =
    file.getAbsolutePath.replace('\\', '/')

}
