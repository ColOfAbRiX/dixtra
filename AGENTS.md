# AGENTS.md

Guidance for AI agents (and contributors) working on dixtra, an opinionated formatter for
Scala 3. For the contributor workflow (issues, PRs, code of conduct) see
[CONTRIBUTING](.github/CONTRIBUTING.md).

## Architecture

Multi-module sbt 2 build (root `build.sbt`), Scala 3.9.0:

| Module          | Directory           | Role                                                                                                   |
| --------------- | ------------------- | ------------------------------------------------------------------------------------------------------ |
| `dixtra` (root) | `.`                 | Aggregates the modules; `run` delegates to the CLI.                                                    |
| `dixtraCore`    | `dixtra-core/`      | The formatter: parsing (scalameta), rules, rendering, settings. No I/O.                                |
| `dixtraCli`     | `dixtra-cli/`       | Config files, file discovery, reports, on top of core. Builds as a GraalVM native image.               |
| `sbtDixtra`     | `dixtra-sbt/`       | sbt 2 plugin. Resolves `dixtra-cli` at task run time in its own classloader, no build-time dependency. |
| `dixtraVsCode`  | `dixtra-vscode/`    | VS Code extension; npm build via `vscodePackage`.                                                      |
| `dixtraBench`   | `dixtra-bench/`     | JMH benchmarks over a frozen corpus. Not published.                                                    |
| —               | `dixtra-microsite/` | **Separate sbt 1.13 build** (Laika), not aggregated by the root.                                        |

Dependency direction: front-ends (`cli`, `sbt`, `vscode`, `bench`) → `core`. `core` must stay
independent of I/O and of the front-ends.

Golden fixtures under `dixtra-cli/src/test/resources/golden` (each: `.dixtra.conf`,
`input.scala.txt`, `expected.scala.txt`) plus the dogfooding spec pin the formatter's behaviour.

## Build and verify

Commands from the repo root; the project convention is
`sbt --client --color=false --supershell=false --batch "<command>"` (plain `sbt <command>`
is equivalent):

```
sbt test                       # all tests, all modules
sbt dixtraCheckAll             # project must be formatted by dixtra itself
sbt dixtraFormatAll            # format the project's own sources in place
sbt "dixtraCore/testOnly <spec>"
sbt sbtDixtra/scripted         # sbt plugin scripted tests
sbt dixtraCli/nativeImage      # GraalVM native executable (needs GRAALVM_HOME)
sbt dixtraVsCode/vscodePackage # VS Code extension (.vsix into dist/)
```

Verification rules:

- After editing source, compile and run the relevant tests and report the actual results;
  do not assert correctness without a green run.
- Verify locally only: never commit, push, or make remote calls as part of testing a change.
- Do not run formatters/scalafix autocorrect as a drive-by; run `dixtraFormatAll` when
  explicitly asked or before pushing when asked to push.
- `publishSigned`/`sonaUpload` cannot run locally (need signing credentials and Central
  access); validate with the tasks above and leave publishing to CI.

## Non-obvious failure modes

- **GraalVM major must match** between the agent that generates `reachability-metadata.json`
  and the image builder (both 25 in CI, `project/NativeImage.scala`). A mismatch makes
  `resources` sections unreadable and every config load fails at runtime
  (`aliases.conf`, `reference.conf` not embedded).
- **Never hardcode absolute tool paths** in build definitions (e.g. `vswhere.exe`); resolve
  from `ProgramFiles(x86)`, `ProgramFiles`, `LOCALAPPDATA`, `PATH` and fail with a clear
  message.
- **`publishLocal` with the fixed version `1.0.0-RC1`**: ivy refuses to overwrite a
  non-SNAPSHOT `ivy.xml`. Remove the `~/.ivy2/local/com.colofabrix.scala/<version>`
  directory (a plain `mv` works if a shell guard blocks `rm -rf`) before republishing.
- **Microsite build**: `sbt --no-global site` from `dixtra-microsite/` — Laika renders the
  markdown under `docs/`, then `site` copies the scaladoc staged by the root build into
  `target/docs/site/api/{core,cli}`. The stage comes first (`sbt copyApiDocsToSite` from the
  root), otherwise the task fails with a message naming the missing directories. `--no-global`
  is needed locally because the global sbt config sets `rootOutputDirectory`, a root-build-only
  key, and the sub-build fails to load without it. Output lands in
  `dixtra-microsite/target/docs/site`.
- **Microsite publish is unverified until gh-pages moves**: the workflow deploys with
  `peaceiris/actions-gh-pages`; treat a publish as done only when the `gh-pages` head moved to
  a new commit, never from the workflow step status.
- **CI: verify job logs, not the workflow summary.** Open each job and step (especially
  platform matrix jobs) and confirm the smoke-test steps passed.
- **Maven Central throttles GitHub runners** on sbt launcher downloads; the microsite
  workflow needs an explicit cache over `~/.sbt/boot`, `~/.cache/coursier/v1`,
  `~/.coursier/v1` or the launcher retries for ~17 minutes and fails with
  `ResolutionError$CantDownloadModule`.
- **GitHub Actions `if:` expressions** in this repo's workflows use single-quoted string
  literals; a double-quoted `"pull_request"` invalidates the whole workflow file.

## Documentation

- Settings reference pages: `dixtra-microsite/docs/docs/reference/*.md`.
  `call-site.md` and `overflow.md` structure each setting as a backticked
  `` ## `group.setting` `` heading, description with inline default, one shared "Before:"
  snippet, then one `` ### `group.setting` = `value` `` subsection per value, with no
  "The settings" table. The other group pages use per-setting `##` headings with
  "With ..." examples and keep a "The settings" table. `path-overrides.md` documents a
  config section and keeps its own shape.
- Doc pages are plain markdown: Laika renders them, but the code samples are not compiled,
  so they are not checked against the library.
- The theme lives in `dixtra-microsite/build.sbt` (Helium: colors, layout, top navigation,
  landing page, footer); page order comes from the `directory.conf` files next to the pages,
  not from a menu file.
- Internal links are validated at build time: a link to a missing page or a wrong anchor fails
  `sbt --no-global site` with the page and the target named, so link references cannot rot.
- The published version appears literally in the install snippets of `docs/usage.md` and
  `docs/library.md` (Laika does not substitute variables inside code blocks): bump those
  three lines together with `DocsVersion`.
- Build with `site`, never `laikaSite` alone: `laikaSite` renders the pages and deletes the
  copied scaladoc, while `site` renders and then copies it back. To start from scratch use
  `sbt clean`, not a hand-deleted `target/docs`, because Laika caches its render on the inputs.
- The README stays lean; the microsite is the enriched developer-facing version.
- User-facing option docs also live in
  `dixtra-cli/src/main/resources/reference.conf`; keep reference pages and it consistent.
- `dixtra-microsite/project/build.properties` and `project/plugins.sbt` must stay tracked:
  the sbt 1.13 sub-build cannot load in a clean checkout without them.

## Code style

- Favour a pure FP approach: cats and cats-effect over OOP.
- Split long expressions into named intermediate `val`s; do not pack many expressions into
  one line or one long nested expression.
- When destructuring a tuple, using tupled syntax, or calling the same function twice,
  bind each component to its own descriptively named `val` — no left-to-right placeholder
  names or inline anonymous tuples.
- Tests: never assert on a single field of an ADT — build the whole expected value and
  compare. Structure each test as input vals → expected val → call → assertion, with no
  comments.
- Minimal comments: explain non-obvious decisions only; good naming does the rest.
- Pureconfig field mapping: CamelCase→KebabCase splits at letter/digit boundaries; if a
  `Config` field name would mis-map, override the mapping in `ConfigInstances` (see
  `compat213`).
