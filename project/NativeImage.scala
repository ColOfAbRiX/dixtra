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

        // The lazy vals of the Scala runtime use sun.misc.Unsafe: on JDK 24 and later every run warns about it and
        // this option silences it. Before 24 the option does not exist and native-image fails on it, so it is only
        // passed when the JVM running sbt, the same one native-image is hosted on, is new enough to know it.
        val unsafeMemoryAccess =
          if javaMajor >= 24 then List("-J--sun-misc-unsafe-memory-access=allow") else Nil

        // The arguments go through a file: on Windows the classpath is too long for a command line
        val arguments =
          List("-march=compatibility") ++
            unsafeMemoryAccess ++
            List(
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
        val command = List(path(tool(if windows then "native-image.cmd" else "native-image")), "@" + path(argumentFile))
        run(log, if windows then inMsvcEnvironment(directory, command) else command)

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
            val agentOptions =
              List(path(tool("java")), s"-agentlib:native-image-agent=config-merge-dir=${path(metadata)}")

            val launchOptions = List("-cp", classpath.map(path).mkString(java.io.File.pathSeparator), mainClass)
            val command       = agentOptions ++ launchOptions ++ arguments

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

  // The major version of the JVM running sbt: "21", or the old scheme "1.8"
  private def javaMajor: Int =
    sys.props.getOrElse("java.specification.version", "21").split('.').last.toIntOption.getOrElse(21)

  private def tool(name: String): File =
    file(graalVmHome) / "bin" / name

  private def graalVmHome: String =
    sys.env.getOrElse("GRAALVM_HOME", javaHomeWithNativeImage)

  private def javaHomeWithNativeImage: String =
    val home       = sys.props("java.home")
    val executable = if windows then "native-image.cmd" else "native-image"
    if (file(home) / "bin" / executable).exists then home
    else sys.error("GRAALVM_HOME must point to a GraalVM installation with a native-image tool")

  private def inMsvcEnvironment(directory: File, command: Seq[String]): Seq[String] =
    val installation =
      Process(
        vSWhere.getPath +: List(
          "-latest",
          "-products",
          "*",
          "-requires",
          "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
          "-property",
          "installationPath",
        ),
      )
        .lazyLines
        .headOption
        .getOrElse(
          sys.error(
            "no Visual Studio installation with the C++ build tools: install them or run sbt from a Native Tools prompt",
          ),
        )
    val vcvars = file(installation) / "VC" / "Auxiliary" / "Build" / "vcvars64.bat"
    val commandLine =
      command.map(argument => if argument.contains(" ") then "\"" + argument + "\"" else argument).mkString(" ")
    val script = directory / "run-native-image.cmd"
    IO.writeLines(
      script,
      Seq(
        "@echo off",
        s"""call "$vcvars"""",
        s"""set "JAVA_HOME=$graalVmHome"""",
        commandLine,
      ),
    )
    List("cmd", "/c", script.getPath)

  private def vSWhere: File =
    val where = file("C:\\Program Files (x86)\\Microsoft Visual Studio\\Installer\\vswhere.exe")
    if where.exists then where else sys.error("vswhere.exe not found: install the Visual Studio C++ build tools")

  private def run(log: Logger, command: Seq[String]): Unit =
    val exit = Process(command).!(ProcessLogger(log.info(_), log.warn(_)))
    if exit != 0 then
      sys.error(s"${command.headOption.getOrElse("")} failed with exit code $exit")

  // Forward slashes: an argument file reads a backslash as an escape
  private def path(file: File): String =
    file.getAbsolutePath.replace('\\', '/')

}
