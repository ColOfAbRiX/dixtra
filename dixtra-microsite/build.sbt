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

// Deep slate chrome with a burnt-amber accent: the palette of a code editor rather than the stock Helium teal, so the
// Scala snippets and the settings reference pages stay the focus. The light and dark schemes are both configured
// explicitly, otherwise dark-mode readers fall back to Helium's own teal palette.
lazy val docsTheme =
  Helium.defaults
    .all.metadata(
      title = Some("Dixtra"),
      description = Some("An opinionated formatter for Scala 3"),
      version = Some(DocsVersion),
      language = Some("en"),
    )
    .all
    .themeColors(
      primary = Color.hex("1e293b"),
      secondary = Color.hex("c2410c"),
      primaryMedium = Color.hex("cbd5e1"),
      primaryLight = Color.hex("f1f5f9"),
      text = Color.hex("1f2937"),
      background = Color.hex("ffffff"),
      bgGradient = (Color.hex("0f172a"), Color.hex("1e293b")),
    )
    .all
    .syntaxHighlightingColors(
      base = ColorQuintet(
        Color.hex("0f172a"),
        Color.hex("94a3b8"),
        Color.hex("cbd5e1"),
        Color.hex("bae6fd"),
        Color.hex("f8fafc"),
      ),
      wheel = ColorQuintet(
        Color.hex("f472b6"),
        Color.hex("fb923c"),
        Color.hex("fcd34d"),
        Color.hex("86efac"),
        Color.hex("38bdf8"),
      ),
    )
    .site
    .darkMode
    .themeColors(
      primary = Color.hex("e2e8f0"),
      secondary = Color.hex("fb923c"),
      primaryMedium = Color.hex("334155"),
      primaryLight = Color.hex("1e293b"),
      text = Color.hex("e5e7eb"),
      background = Color.hex("0f172a"),
      bgGradient = (Color.hex("020617"), Color.hex("0f172a")),
    )
    .site
    .darkMode
    .syntaxHighlightingColors(
      base = ColorQuintet(
        Color.hex("020617"),
        Color.hex("94a3b8"),
        Color.hex("cbd5e1"),
        Color.hex("bae6fd"),
        Color.hex("f8fafc"),
      ),
      wheel = ColorQuintet(
        Color.hex("f472b6"),
        Color.hex("fb923c"),
        Color.hex("fcd34d"),
        Color.hex("86efac"),
        Color.hex("38bdf8"),
      ),
    )
    .site
    .layout(
      contentWidth = px(920),
      navigationWidth = px(265),
      defaultBlockSpacing = px(14),
      defaultLineHeight = 1.6,
    )
    .site
    .fontSizes(
      body = px(16),
      code = px(14),
      title = px(32),
      header2 = px(24),
      header3 = px(18),
      header4 = px(16),
      small = px(13),
    )
    .site
    .internalCSS(Root / "site.css")
    .site
    .topNavigationBar(
      navLinks = Seq(
        TextLink.internal(Root / "docs" / "index.md", "Docs"),
        TextLink.internal(Root / "docs" / "reference" / "braces.md", "Reference"),
        TextLink.internal(Root / "docs" / "api.md", "API"),
        IconLink.external("https://github.com/ColOfAbRiX/dixtra", HeliumIcon.github),
      )
    )
    .site
    .mainNavigation(depth = 3)
    .site
    .pageNavigation(depth = 1)
    .site
    .footer("Dixtra — An opinionated formatter for Scala 3 · MIT licensed")
    .site
    .landingPage(
      title = Some("Dixtra"),
      subtitle = Some("An opinionated formatter for Scala 3"),
      latestReleases = Seq(ReleaseInfo("Latest release", DocsVersion)),
      license = Some("MIT"),
      titleLinks = Seq(
        ButtonLink.internal(Root / "docs" / "usage.md", "Get started"),
        ButtonLink.internal(Root / "docs" / "reference" / "braces.md", "Settings reference"),
      ),
      linkPanel = Some(
        LinkPanel(
          "Reference",
          TextLink.internal(Root / "docs" / "reference" / "braces.md", "Braces"),
          TextLink.internal(Root / "docs" / "reference" / "body.md", "Bodies"),
          TextLink.internal(Root / "docs" / "reference" / "selector-chain.md", "Selector chains"),
          TextLink.internal(Root / "docs" / "reference" / "alignment.md", "Alignment"),
          TextLink.internal(Root / "docs" / "reference" / "call-site.md", "Call sites"),
          TextLink.internal(Root / "docs" / "reference" / "definition-site.md", "Definition sites"),
          TextLink.internal(Root / "docs" / "reference" / "overflow.md", "Overflow"),
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
