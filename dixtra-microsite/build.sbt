// Dixtra documentation microsite.
//
// This is a standalone sbt 1.x build, independent from the main build in the repository root (which uses sbt 2.x).
// Building the site requires Ruby and Jekyll on the system.
//
// The pages are plain markdown: mdoc renders them, but the code samples are not compiled, so they are not checked
// against the library. Bump DocsVersion at every release.

lazy val DocsVersion = "1.0.0-RC1"

lazy val copyApiDocsToSite = taskKey[Unit]("Copies the staged API docs into the built site")

lazy val dixtraMicrosite =
  project
    .in(file("."))
    .enablePlugins(MicrositesPlugin)
    .settings(
      scalaVersion                := "3.9.0",
      organization                := "com.colofabrix.scala",
      name                        := "dixtra-microsite",
      version                     := DocsVersion,
      publish / skip              := true,
      micrositeAuthor             := "ColOfAbRiX",
      micrositeBaseUrl            := "dixtra",
      micrositeDescription        := "An opinionated formatter for Scala 3",
      micrositeDocumentationUrl   := "docs/",
      micrositeGithubOwner        := "ColOfAbRiX",
      micrositeGithubRepo         := "dixtra",
      micrositeGithubToken        := sys.env.get("GITHUB_TOKEN"),
      micrositeGitterChannel      := false,
      micrositeHighlightLanguages ++= Seq("scala", "conf", "text", "shell"),
      micrositeHighlightTheme     := "atom-one-dark", // https://highlightjs.org/static/demo/
      micrositeHomepage           := "https://colofabrix.github.io/dixtra/",
      micrositeName               := "Dixtra",
      micrositePushSiteWith       := GitHub4s,
      mdocVariables := Map(
        "VERSION" -> DocsVersion,
      ),
      // Host the API documentation on GitHub Pages: the scaladoc of the formatter under /api/core, and the one of the
      // command line under /api/cli. The docs are staged by the main build (sbt copyApiDocsToSite, run from the
      // repository root).
      copyApiDocsToSite := {
        val stagingDir = baseDirectory.value / "api-docs"
        val siteDir    = (makeSite / target).value
        IO.copyDirectory(stagingDir / "core", siteDir / "api" / "core", overwrite = true)
        IO.copyDirectory(stagingDir / "cli", siteDir / "api" / "cli", overwrite = true)
      },
      publishMicrosite := Def.sequential(clean, makeMicrosite, copyApiDocsToSite, pushMicrosite).value,
    )
