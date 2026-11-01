import org.typelevel.scalacoptions.ScalacOptions
import scala.sys.process.*

// Released with the Scala 3 LTS version
val scala3Version = "3.9.0"

// The sbt plugin is built for sbt 2.0, with the Scala version sbt 2.0 runs on
val sbtScalaVersion = "3.8.4"

ThisBuild / organization := "com.colofabrix.scala"

Global / run / fork := true
Global / tpolecatExcludeOptions ++=
  Set(
    ScalacOptions.warnUnusedImports,
    ScalacOptions.warnUnusedPrivates,
    ScalacOptions.warnUnusedLocals,
    ScalacOptions.privateKindProjector,
  )

// The settings of the Scala 3 projects
lazy val scala3Settings =
  Seq(
    scalaVersion  := scala3Version,
    scalacOptions += "-Wall",
    // The compile-only options (plugins, wartremover, semanticdb) only produce warnings in scaladoc
    Compile / doc / scalacOptions := Seq("-project", name.value, "-project-version", version.value),
    semanticdbEnabled             := true,
    semanticdbVersion             := scalafixSemanticdb.revision,
    javaOptions                   += "-Dfile.encoding=UTF-8",
    Test / fork                   := true,
    Compile / wartremoverErrors   := Warts.unsafe,
    Test / wartremoverErrors      := Nil,
    Test / wartremoverWarnings    := Nil,
    Test / scalacOptions ~= (_.filterNot(_.startsWith("-P:wartremover:"))),
  )

// The build: it only aggregates the projects. `run` runs the command line.
lazy val dixtra =
  project
    .in(file("."))
    .aggregate(dixtraCore, dixtraCli, sbtDixtra, dixtraVsCode)
    .settings(
      name                      := "dixtra",
      scalaVersion              := scala3Version,
      publish / skip            := true,
      Compile / run / aggregate := false,
      Compile / run             := (dixtraCli / Compile / run).evaluated,
    )

// The formatter alone: the formatting of a source text by a configuration
lazy val dixtraCore =
  project
    .in(file("dixtra-core"))
    .settings(scala3Settings)
    .settings(
      name    := "dixtra-core",
      version := "1.0.0-RC1",
      libraryDependencies ++= List(
        "org.scalameta" %% "scalameta"                % "4.17.4",
        "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.20" % Test,
        "org.scalatest" %% "scalatest-wordspec"       % "3.2.20" % Test,
      ),
    )

// The command line: the configuration files, the files to format and the reports, on top of the formatter.
// `nativeImage` builds it as a GraalVM native executable (see project/NativeImage.scala).
lazy val dixtraCli =
  project
    .in(file("dixtra-cli"))
    .dependsOn(dixtraCore % "compile->compile;test->test")
    .enablePlugins(BuildInfoPlugin)
    .settings(scala3Settings)
    .settings(
      name                := "dixtra-cli",
      version             := "1.0.0-RC1",
      buildInfoKeys       := Seq[BuildInfoKey](version),
      buildInfoPackage    := "com.colofabrix.scala.dixtra",
      wartremoverExcluded += (Compile / sourceManaged).value,
      libraryDependencies ++= List(
        "com.colofabrix.scala"  %% "declinio"                      % "1.0.0",
        "com.github.pureconfig" %% "pureconfig-core"               % "0.17.10",
        "com.github.pureconfig" %% "pureconfig-generic-scala3"     % "0.17.10",
        "com.monovore"          %% "decline"                       % "2.6.2",
        "org.scalatest"         %% "scalatest-shouldmatchers"      % "3.2.20" % Test,
        "org.scalatest"         %% "scalatest-wordspec"            % "3.2.20" % Test,
        "org.typelevel"         %% "cats-core"                     % "2.13.0",
        "org.typelevel"         %% "cats-effect-testing-scalatest" % "1.8.0"  % Test,
        "org.typelevel"         %% "cats-effect"                   % "3.7.0",
      ),
    )
    .settings(
      NativeImage.settings("com.colofabrix.scala.dixtra.Main", "dixtra"),
    )
    .settings(
      // The runs traced for the native-image metadata: every option, and each kind of result, on a copy of the sources
      NativeImage.nativeImageRuns := {
        val build = (ThisBuild / baseDirectory).value
        scratch => {
          val sources = scratch / "src"
          val src     = sources.getAbsolutePath

          IO.copyDirectory(build / "dixtra-core" / "src" / "main" / "scala", sources / "core")
          IO.copyDirectory(build / "dixtra-cli" / "src" / "main" / "scala", sources / "cli")
          IO.copyFile(build / "build.sbt", sources / "build.sbt")
          IO.write(sources / ".dixtra.conf", "config-version = 1.0.0\nscaladoc.align-tags = true\n")
          IO.write(scratch / "Old.scala", "object Old {\n  def f() { println(1) }\n}\n")
          IO.write(scratch / "Broken.scala", "object Broken {\n")

          Seq(
            Seq("--version"),
            Seq("--help"),
            Seq(),
            Seq("--check", src),
            Seq(src),
            Seq("--config-string", "max-column = 80, braces.style = allman", "-w", "100", src),
            Seq("--config-path", (sources / ".dixtra.conf").getAbsolutePath, "-c", src),
            Seq("--config-string", "unknown-key = 1", src),
            Seq((scratch / "Old.scala").getAbsolutePath, (scratch / "Broken.scala").getAbsolutePath),
            Seq("--check", "--changed-since", "HEAD", (build / "dixtra-cli" / "src").getAbsolutePath),
          )
        }
      },
    )

// The sbt plugin. It does not depend on dixtra at build time: it resolves dixtra when it runs and loads it in a class
// loader of its own, so that dixtra keeps its Scala version inside sbt.
lazy val sbtDixtra =
  project
    .in(file("dixtra-sbt"))
    .enablePlugins(SbtPlugin, BuildInfoPlugin)
    .settings(
      name                          := "sbt-dixtra",
      version                       := "1.0.0-RC1",
      scalaVersion                  := sbtScalaVersion,
      semanticdbEnabled             := true,
      semanticdbVersion             := scalafixSemanticdb.revision,
      Compile / doc / scalacOptions := Seq("-project", name.value, "-project-version", version.value),
      buildInfoPackage              := "com.colofabrix.scala.sbtdixtra",
      buildInfoKeys :=
        Seq[BuildInfoKey](organization, version, "dixtraModule" -> (dixtraCli / moduleName).value),
      scriptedLaunchOpts ++=
        Seq("-Xmx1024M", s"-Dplugin.version=${version.value}") ++
        // The scripted builds do not inherit the options of the sbt launcher (`/etc/sbt/sbtopts`): they are given the
        // redirected homes of the caches again, when the launcher runs with them.
        Seq("sbt.ivy.home", "sbt.boot.directory", "sbt.coursier.home", "sbt.global.base").flatMap { key =>
          Option(System.getProperty(key)).map(value => s"-D$key=$value")
        },
      scriptedBufferLog    := false,
      scriptedDependencies := scriptedDependencies.dependsOn(dixtraCore / publishLocal, dixtraCli / publishLocal).value,
    )

// The VS Code extension: it formats the document of the editor with dixtra on Format Document, and npm builds it in
// this folder. `vscodePackage` installs the npm dependencies, compiles the extension and packages it into a `.vsix`,
// again when its sources change.
lazy val dixtraVsCode =
  project
    .in(file("dixtra-vscode"))
    .settings(
      name           := "dixtra-vscode",
      version        := "1.0.0-RC1",
      scalaVersion   := scala3Version,
      publish / skip := true,
      vscodePackage := {
        val folder = baseDirectory.value

        // The version of the extension follows the version of the build: when it differs, the first `version` field of
        // `package.json` (the first of the file, the top-level one) is rewritten with it, and committed with it.
        val manifest = folder / "package.json"
        val current  = IO.read(manifest)
        val pattern  = """("version" *: *")[^"]+(")""".r
        val synced   = pattern.replaceFirstIn(current, s"$$1${version.value}$$2")

        if synced != current then
          IO.write(manifest, synced)

        val license = (ThisBuild / baseDirectory).value / "LICENSE"
        val copy    = folder / "LICENSE"

        if !copy.exists() || IO.read(copy) != IO.read(license) then
          IO.copyFile(license, copy)

        // The outputs of the extension, all in `dist/`: the executables bundled with it and its icon. The native builds
        // of `dixtraCli` write into the shared drive: the one of the current platform first and the ones of the other
        // platforms alongside, in the per-platform layouts of sbt, so the extension picks up every executable that is
        // present and warns about the ones that are not.
        val log              = streams.value.log
        val nativeExecutable = (dixtraCli / NativeImage.nativeImage).value
        val nativeOutputs =
          val out   = (ThisBuild / baseDirectory).value / "target" / "out"
          val roots = (out / "jvm") +: (out * "*").get().filter(_.isDirectory).map(_ / "jvm")

          (nativeExecutable.getParentFile +: roots.flatMap(root => (root * "scala-*" * "*" * "native").get()))
            .filter(_.isDirectory)
            .distinct

        val dist = (ThisBuild / baseDirectory).value / "dist"

        def copyExecutable(platform: String, name: String, destination: File): Unit =
          nativeOutputs
            .find(output => (output / name).exists())
            .map(_ / name) match {
            case Some(source) =>
              if !destination.exists() || IO.readBytes(source) != IO.readBytes(destination) then
                IO.createDirectory(destination.getParentFile)
                IO.copyFile(source, destination)
                destination.setExecutable(true, false)
            case None =>
              log.warn(
                s"no $platform executable $name among ${nativeOutputs.mkString(", ")}: the extension is packaged without it",
              )
          }

        copyExecutable("Linux", "dixtra", dist / "linux-x64" / "dixtra")
        copyExecutable("Windows", "dixtra.exe", dist / "win32-x64" / "dixtra.exe")

        // The icon of the extension, rendered from the SVG source: the first renderer that works wins. Without a
        // renderer a previously rendered icon is reused, and with none at all the packaging fails.
        val icon = dist / "icon.png"

        def renderIcon: Boolean =
          IO.createDirectory(dist)
          val commands =
            Seq(
              Seq("rsvg-convert", "-w", "256", "-h", "256", (folder / "icon.svg").getPath, "-o", icon.getPath),
              Seq("convert", "-background", "none", (folder / "icon.svg").getPath, "-resize", "256x256", icon.getPath),
              Seq(
                "npx",
                "--yes",
                "@resvg/resvg-js-cli",
                (folder / "icon.svg").getPath,
                icon.getPath,
                "--fit-width",
                "256",
                "--fit-height",
                "256",
              ),
            )

          commands.exists { command =>
            log.info(s"rendering the icon with ${command.head}")
            scala.util.Try(Process(command, Some(folder)).!).toOption.contains(0) && icon.exists()
          }

        if !renderIcon && !icon.exists() then
          throw new MessageOnlyException(
            "The icon of the extension could not be rendered: no renderer among rsvg-convert, convert and resvg, and no previously rendered one",
          )

        // What `vsce` packages must live inside the extension folder: the Windows executable and the icon of `dist` are
        // staged into `bin` before packaging (nothing from `dist` reaches the repository, it is ignored).
        val payload = folder / "bin"
        val windows = dist / "win32-x64" / "dixtra.exe"

        if windows.exists() then
          IO.createDirectory(payload / "win32-x64")
          IO.copyFile(windows, payload / "win32-x64" / "dixtra.exe")

        if icon.exists() then
          IO.createDirectory(payload)
          IO.copyFile(icon, payload / "icon.png")

        // The inputs of the build: the sources of the extension, its manifest and configuration, the icon source, and
        // the executables and icon bundled above. `icon.png` is an output, not an input: it changes only when
        // `icon.svg` does.
        val sources =
          ((folder / "src") ** "*.ts").get() ++
          Seq(folder / "package.json", folder / "tsconfig.json", folder / "icon.svg") ++
          Seq(dist / "linux-x64" / "dixtra", dist / "win32-x64" / "dixtra.exe").filter(_.exists()) ++
          (if icon.exists() then Seq(icon) else Seq.empty)

        val cached =
          FileFunction.cached(streams.value.cacheDirectory / "vscode-package", FilesInfo.hash) { _ =>
            npmBuild(folder, streams.value.log, "install", "--no-fund", "--no-audit")
            npmBuild(folder, streams.value.log, "run", "compile")
            npmBuild(folder, streams.value.log, "run", "package")
            (folder * "*.vsix").get().toSet
          }

        val produced =
          cached(sources.toSet)
            .toList
            .sortBy(_.lastModified)
            .lastOption

        produced.getOrElse(throw new MessageOnlyException("The build of the extension produced no .vsix"))
      },
    )

// The build task of the VS Code extension in `dixtra-vscode/`
@transient
lazy val vscodePackage =
  taskKey[File]("Package the VS Code extension into a .vsix")

def npmBuild(folder: File, log: Logger, arguments: String*): Unit =
  val npm =
    if sys.props("os.name").toLowerCase.contains("windows") then
      Seq("cmd", "/c", "npm") ++ arguments
    else
      Seq("npm") ++ arguments

  log.info(s"npm ${arguments.mkString(" ")}")

  val exitCode = Process(npm, Some(folder)).!
  if exitCode != 0 then
    throw new MessageOnlyException(s"npm ${arguments.mkString(" ")} failed with exit code $exitCode")
