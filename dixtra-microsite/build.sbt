// Dixtra documentation site.
//
// This is a standalone sbt 1.x build, independent from the main build in the repository root (which uses sbt 2.x):
// Laika's sbt plugin has no sbt 2 release. The site is rendered by Laika alone, with no Jekyll and no Ruby.
//
// The pages are plain markdown: the code samples are not compiled, so they are not checked against the library.
// Bump DocsVersion at every release.

import laika.ast.LengthUnit.px
import laika.ast.Path.Root
import laika.config.SyntaxHighlighting
import laika.format.Markdown
import laika.helium.Helium
import laika.helium.config._
import laika.theme.config.Color

lazy val DocsVersion = "1.0.0-RC1"

lazy val copyApiDocs = taskKey[Unit]("Copies the staged API documentation into the built site")
lazy val site        = taskKey[Unit]("Builds the documentation site, the API documentation included")

lazy val docsTheme =
  Helium.defaults
    .all.metadata(
      title = Some("Dixtra"),
      description = Some("An opinionated formatter for Scala 3"),
      version = Some(DocsVersion),
      language = Some("en"),
    )
    .site
    .layout(contentWidth = px(900))
    .site
    .topNavigationBar(
      navLinks = Seq(
        TextLink.internal(Root / "docs" / "api.md", "API"),
        IconLink.external("https://github.com/ColOfAbRiX/dixtra", HeliumIcon.github),
      )
    )
    .site
    .mainNavigation(depth = 3)
    .site
    .footer("An opinionated formatter for Scala 3, MIT licensed.")
    .site
    .landingPage(
      title = Some("Dixtra"),
      subtitle = Some("An opinionated formatter for Scala 3"),
      latestReleases = Seq(ReleaseInfo("Latest release", DocsVersion)),
      license = Some("MIT"),
      titleLinks = Seq(
        ButtonLink.internal(Root / "docs" / "usage.md", "Get started"),
      ),
      linkPanel = Some(
        LinkPanel(
          "Settings",
          TextLink.internal(Root / "docs" / "reference" / "braces.md", "Braces"),
          TextLink.internal(Root / "docs" / "reference" / "call-site.md", "Call sites"),
          TextLink.internal(Root / "docs" / "reference" / "alignment.md", "Alignment"),
        )
      ),
      projectLinks = Seq(
        IconLink.external("https://github.com/ColOfAbRiX/dixtra", HeliumIcon.github),
      ),
      teasers = Seq(
        Teaser(
          "Opinionated",
          "Dixtra formats the code as a set of visual features, not as a set of typographic rules you have to tune.",
        ),
        Teaser(
          "One formatter",
          "The sbt plugin, the command line and the editor extension all run the same formatter, with the same configuration.",
        ),
        Teaser(
          "Configurable",
          "A few settings change the layout of the constructs you care about, and nothing else.",
        ),
      ),
    )
    .build

lazy val dixtraMicrosite =
  project
    .in(file("."))
    .enablePlugins(LaikaPlugin)
    .settings(
      name                      := "dixtra-microsite",
      version                   := DocsVersion,
      publish / skip            := true,
      Laika / sourceDirectories := Seq(baseDirectory.value / "docs"),
      laikaExtensions           := Seq(Markdown.GitHubFlavor, SyntaxHighlighting),
      laikaTheme                := docsTheme,
      // The scaladoc is not part of the Laika inputs (it is copied verbatim, not parsed): the main build stages it in
      // api-docs/, and copyApiDocs drops it into the built site under /api/core and /api/cli.
      copyApiDocs := {
        val stagingDir = baseDirectory.value / "api-docs"
        val siteDir    = (Laika / target).value / "site"
        val coreApi    = stagingDir / "core"
        val cliApi     = stagingDir / "cli"
        if (!coreApi.exists() || !cliApi.exists())
          sys.error(
            s"The API documentation is not staged: run `sbt copyApiDocsToSite` from the repository root first (expected $coreApi and $cliApi)."
          )
        IO.copyDirectory(coreApi, siteDir / "api" / "core", overwrite = true)
        IO.copyDirectory(cliApi, siteDir / "api" / "cli", overwrite = true)
      },
      site := Def.sequential(laikaSite, copyApiDocs).value,
    )
