package com.colofabrix.scala.sbtdixtra

import java.io.File
import java.net.URLClassLoader
import java.util.function.Consumer
import sbt.*
import sbt.Keys.*
import scala.collection.concurrent.TrieMap

/**
 * Formats the Scala sources and the build definition of a project with dixtra.
 *
 * dixtra is not a dependency of the plugin: the plugin resolves it (version `dixtraVersion`) when a task runs and loads
 * it in a class loader of its own, so that dixtra runs on its own Scala version whatever the version of sbt. It runs
 * the dixtra command line, and so reads the nearest `.dixtra.conf` of each file unless `dixtraConfig` names a
 * configuration file.
 *
 * `dixtraFormat` and `dixtraCheck` work on the sources of a configuration (`Compile` by default, `Test / dixtraFormat`
 * for the tests); a custom configuration gets them with `inConfig(MyConfig)(DixtraPlugin.configSettings)`.
 */
object DixtraPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = plugins.JvmPlugin

  /**
   * The settings and tasks of the plugin.
   */
  object autoImport {
    val dixtraVersion: SettingKey[String] =
      settingKey[String]("The version of dixtra to format with")
    val dixtraConfig: SettingKey[Option[File]] =
      settingKey[Option[File]]("A configuration file, instead of the nearest .dixtra.conf of each file")
    val dixtraChangedSince: SettingKey[Option[String]] =
      settingKey[Option[String]]("A git ref: only the files changed since it, instead of all")
    val dixtraOnCompile: SettingKey[Boolean] =
      settingKey[Boolean]("Format the sources of a configuration before compiling them")
    val dixtraClasspath: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("The jars of dixtra and its dependencies")
    val dixtraFormat: TaskKey[Unit] =
      taskKey[Unit]("Format the Scala sources of a configuration")
    val dixtraCheck: TaskKey[Unit] =
      taskKey[Unit]("Fail when a Scala source of a configuration is not formatted")
    val dixtraFormatAll: TaskKey[Unit] =
      taskKey[Unit]("Format the Scala sources of every configuration, and the build files")
    val dixtraCheckAll: TaskKey[Unit] =
      taskKey[Unit]("Fail when a Scala source of any configuration, or a build file, is not formatted")
    val dixtraFormatSbt: TaskKey[Unit] =
      taskKey[Unit]("Format the build definition: the .sbt files, and the sources in project/")
    val dixtraCheckSbt: TaskKey[Unit] =
      taskKey[Unit]("Fail when a file of the build definition is not formatted")
    val dixtraFormatOnly: InputKey[Unit] =
      inputKey[Unit]("Format the given files")
  }

  import autoImport.*

  // The configurations whose sources the *All tasks cover.
  private val allConfigurations =
    ScopeFilter(configurations = inConfigurations(Compile, Test))

  // The class loaders of dixtra, one for each classpath, kept across runs.
  private val loaders =
    TrieMap.empty[Seq[File], ClassLoader]

  // Formats the sources before they are compiled, when `dixtraOnCompile` is on; a file that fails (e.g. one that does
  // not parse) is left to the compiler.
  private val dixtraOnCompileFormat =
    taskKey[Unit]("Format the sources before compiling, if dixtraOnCompile")

  // What a run does with the files: rewrite them, or report the ones that are not formatted.
  private enum Mode {
    case Format, Check
  }

  // What a run that fails does: fail the task, or only log a warning.
  private enum OnFailure {
    case Fail, Warn
  }

  // The source directories of a configuration, of `Compile` and `Test`, the build definition, and both of the latter.
  private val sourceDirectories =
    Def.setting(existing(unmanagedSourceDirectories.value))
  private val allSourceDirectories =
    Def.setting(existing(unmanagedSourceDirectories.all(allConfigurations).value.flatten))
  private val buildDefinition =
    Def.setting(buildFiles(baseDirectory.value))
  private val allSourcesAndBuild =
    Def.setting(allSourceDirectories.value ++ buildDefinition.value)

  // The label of a run in the log: the project alone for the project-wide tasks, the project and its configuration for
  // the tasks on the sources of a configuration, e.g. `dixtraCore` and `dixtraCore / Compile`.
  private val projectId =
    Def.setting(thisProjectRef.value.project)
  private val projectAndConfig =
    Def.setting(s"${thisProjectRef.value.project} / ${configuration.value.name}")

  // The summary of a run, as `CliReport` of dixtra writes it: `1 file`, or `12 files: 3 formatted, 9 unchanged`.
  private val summary =
    """\d+ files?(:.*)?""".r

  /**
   * The tasks on the sources of a configuration.
   */
  lazy val configSettings: Seq[Setting[?]] =
    Seq(
      dixtraFormat := Def.uncached(dixtraTask(Mode.Format, sourceDirectories, projectAndConfig, OnFailure.Fail).value),
      dixtraCheck  := Def.uncached(dixtraTask(Mode.Check, sourceDirectories, projectAndConfig, OnFailure.Fail).value),
      dixtraOnCompileFormat := Def.uncached {
        Def
          .taskIf {
            if dixtraOnCompile.value then
              dixtraTask(Mode.Format, sourceDirectories, projectAndConfig, OnFailure.Warn).value
            else
              ()
          }
          .value
      },
      sources := Def.uncached(sources.dependsOn(dixtraOnCompileFormat).value),
    )

  override lazy val projectSettings: Seq[Setting[?]] =
    val projectWide =
      Seq(
        dixtraVersion      := BuildInfo.version,
        dixtraConfig       := None,
        dixtraChangedSince := None,
        dixtraOnCompile    := false,
        dixtraFormatAll := Def.uncached(
          dixtraTask(Mode.Format, allSourcesAndBuild, projectId, OnFailure.Fail).value,
        ),
        dixtraCheckAll := Def.uncached(
          dixtraTask(Mode.Check, allSourcesAndBuild, projectId, OnFailure.Fail).value,
        ),
        dixtraFormatSbt := Def.uncached(
          dixtraTask(Mode.Format, buildDefinition, projectId, OnFailure.Fail).value,
        ),
        dixtraCheckSbt := Def.uncached(
          dixtraTask(Mode.Check, buildDefinition, projectId, OnFailure.Fail).value,
        ),
        dixtraClasspath := Def.uncached {
          val module = BuildInfo.organization % s"${BuildInfo.dixtraModule}_3" % dixtraVersion.value
          dependencyResolution
            .value
            .retrieve(module, None, csrCacheDirectory.value, streams.value.log)
            .fold(warning => sys.error(warning.resolveException.getMessage), identity)
        },
        dixtraFormatOnly := {
          val base = baseDirectory.value
          val files =
            Def
              .spaceDelimited("<files>")
              .parsed
              .map(name => IO.resolve(base, new File(name)))
          val args = arguments(Mode.Format, dixtraConfig.value, None, files)
          run(dixtraClasspath.value, args, Mode.Format, OnFailure.Fail, projectId.value, streams.value.log)
        },
      )
    val perConfiguration = inConfig(Compile)(configSettings) ++ inConfig(Test)(configSettings)

    projectWide ++ perConfiguration

  // Run dixtra in `mode` on `paths`, with the settings of the task's scope, logging the run as `label`.
  private def dixtraTask(
    mode: Mode,
    paths: Def.Initialize[Seq[File]],
    label: Def.Initialize[String],
    onFailure: OnFailure,
  ): Def.Initialize[Task[Unit]] =
    Def.task {
      Def.uncached {
        val args = arguments(mode, dixtraConfig.value, dixtraChangedSince.value, paths.value)
        run(dixtraClasspath.value, args, mode, onFailure, label.value, streams.value.log)
      }
    }

  // The directories among `directories` that exist, once each.
  private def existing(directories: Seq[File]): Seq[File] =
    directories.distinct.filter(_.isDirectory)

  // The build definition of a project: its .sbt files, and the .sbt and .scala files directly in its project/
  // directory.
  private def buildFiles(base: File): Seq[File] =
    def files(dir: File, extensions: Set[String]): Seq[File] =
      Option(dir.listFiles())
        .toSeq
        .flatten
        .filter { file =>
          val hasKnownExtension = extensions.exists(file.getName.endsWith)
          file.isFile && hasKnownExtension
        }
        .sortBy(_.getName)

    files(base, Set(".sbt")) ++ files(base / "project", Set(".sbt", ".scala"))

  // The command line of dixtra for `paths`, empty when there is nothing to do.
  private def arguments(mode: Mode, config: Option[File], changedSince: Option[String], paths: Seq[File]): Seq[String] =
    if paths.isEmpty then
      Nil
    else
      val checkFlags   = if mode == Mode.Check then Seq("--check") else Nil
      val configFlags  = config.toSeq.flatMap(file => Seq("--config-path", file.getAbsolutePath))
      val changedFlags = changedSince.toSeq.flatMap(ref => Seq("--changed-since", ref))
      val pathFlags    = paths.map(_.getAbsolutePath)

      checkFlags ++ configFlags ++ changedFlags ++ pathFlags

  // Run dixtra with `args` on behalf of `label`, failing the task or only logging a warning when the run fails. The
  // failure is a message only: the files at fault are already logged.
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def run(
    classpath: Seq[File],
    args: Seq[String],
    mode: Mode,
    onFailure: OnFailure,
    label: String,
    log: Logger,
  ): Unit =
    if args.nonEmpty then
      val code = invoke(classpath, args, label, log)
      (code, onFailure, mode) match {
        case (0, _, _) =>
          ()
        case (_, OnFailure.Warn, _) =>
          log.warn("dixtra: some files could not be formatted")
        case (_, OnFailure.Fail, Mode.Check) =>
          throw new MessageOnlyException("dixtra: some files are not formatted; the dixtraFormat* tasks format them")
        case (_, OnFailure.Fail, Mode.Format) =>
          throw new MessageOnlyException("dixtra: some files could not be formatted")
      }

  // Call `Embedded.run` of dixtra in its own class loader, whose parent is the platform class loader so that nothing of
  // sbt (its Scala library first) is visible to dixtra.
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private def invoke(classpath: Seq[File], args: Seq[String], label: String, log: Logger): Int =
    val loader =
      loaders.getOrElseUpdate(
        classpath,
        new URLClassLoader(classpath.map(_.toURI.toURL).toArray, ClassLoader.getPlatformClassLoader),
      )

    val entry =
      loader
        .loadClass("com.colofabrix.scala.dixtra.Embedded")
        .getMethod("run", classOf[Array[String]], classOf[Consumer[String]], classOf[Consumer[String]])

    // The level of each line by its prefix, as `CliReport` of dixtra writes it: the summary of the run is prefixed with
    // `label`, the files that are unchanged are logged at debug.
    val out: Consumer[String] =
      line =>
        if line.startsWith("unchanged:") then log.debug(line)
        else if summary.matches(line) then log.info(s"$label: $line")
        else log.info(line)

    val err: Consumer[String] =
      line => if line.startsWith("skipped") || line.startsWith("warning") then log.warn(line) else log.error(line)

    val thread   = Thread.currentThread
    val previous = thread.getContextClassLoader
    thread.setContextClassLoader(loader)

    try {
      entry.invoke(null, args.toArray, out, err) match {
        case code: Integer => code.intValue
        case other         => sys.error(s"dixtra: unexpected result ${String.valueOf(other)}")
      }
    } finally {
      thread.setContextClassLoader(previous)
    }

}
