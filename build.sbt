import org.typelevel.scalacoptions.ScalacOptions

// Released with the Scala 3 LTS version
val scala3Version = "3.9.0"

// The sbt plugin is built for sbt 2.0, with the Scala version sbt 2.0 runs on
val sbtScalaVersion = "3.8.4"

ThisBuild / version      := "1.0.0-RC1"
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
    .aggregate(dixtraCore, dixtraCli, sbtDixtra)
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
      name := "dixtra-core",
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
    .settings(NativeImage.settings("com.colofabrix.scala.dixtra.Main", "dixtra"))
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
      scalaVersion                  := sbtScalaVersion,
      semanticdbEnabled             := true,
      semanticdbVersion             := scalafixSemanticdb.revision,
      Compile / doc / scalacOptions := Seq("-project", name.value, "-project-version", version.value),
      buildInfoKeys := Seq[BuildInfoKey](organization, version, "dixtraModule" -> (dixtraCli / moduleName).value),
      buildInfoPackage              := "com.colofabrix.scala.sbtdixtra",
      scriptedLaunchOpts            ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}"),
      scriptedBufferLog             := false,
      scriptedDependencies := scriptedDependencies.dependsOn(dixtraCore / publishLocal, dixtraCli / publishLocal).value,
    )
