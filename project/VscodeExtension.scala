import sbt.*
import sbt.Keys.*
import scala.sys.process.*

/**
 * Packages the VS Code extension of `dixtra-vscode/` into `.vsix` files with `vsce`.
 *
 * The extension formats the document of the editor with dixtra on Format Document, and npm builds it in the folder of
 * the project. There is one `.vsix` per platform, each bundling only the executable of that platform: the
 * `vscodePackage*` tasks stage the executables of `dist/` into `bin/`, install the npm dependencies, compile the
 * extension and package it into a `.vsix`, again when its inputs change, and copy it into the `dist/` folder of the
 * build. `vscodePackage` packages for every platform with an executable, the other tasks for one platform each.
 */
object VsCodeExtension {

  // The build tasks of the VS Code extension in `dixtra-vscode/`
  @transient
  val vscodePackage =
    taskKey[File]("Package the VS Code extension for every platform with an executable, in the output directory")
  @transient
  val vscodePackageLinux =
    taskKey[File]("Package the VS Code extension for linux-x64, in the output directory")
  @transient
  val vscodePackageWin =
    taskKey[File]("Package the VS Code extension for win32-x64, in the output directory")
  @transient
  val vscodePackageMac =
    taskKey[File]("Package the VS Code extension for darwin-arm64, in the output directory")

  // The executable of a platform: the file name in `dist/<platform>/` and `bin/<platform>/`, and the npm script
  // packaging that platform into its `.vsix`
  private final case class Platform(executable: String, script: String)
  private val platforms =
    Map(
      "linux-x64"   -> Platform("dixtra", "package:linux"),
      "win32-x64"   -> Platform("dixtra.exe", "package:win"),
      "darwin-arm64" -> Platform("dixtra", "package:mac"),
    )

  /**
   * The tasks of the extension project: `vscodePackage`, `vscodePackageLinux` and `vscodePackageWin`, which package
   * the extension of `dixtra-vscode/`.
   */
  def settings: Seq[Setting[?]] =
    Seq(
      vscodePackage      := packageExtension(None).value,
      vscodePackageLinux := packageExtension(Some("linux-x64")).value,
      vscodePackageWin   := packageExtension(Some("win32-x64")).value,
      vscodePackageMac   := packageExtension(Some("darwin-arm64")).value,
    )

  /**
   * The build of one `.vsix`: when the executable of every wanted platform is in `dist/`, the package is built from
   * it straight away, otherwise the native image of the current platform is built first, as the only one this machine
   * can build.
   */
  private def packageExtension(target: Option[String]): Def.Initialize[Task[File]] =
    Def.taskDyn {
      val dist = (ThisBuild / baseDirectory).value / "dist"
      def present(platform: String) = (dist / platform / platforms(platform).executable).exists()
      // A package for one platform needs its executable; the package of every platform needs some executable to be
      // worth building, and warns about the platforms without one.
      val complete = target.fold(platforms.keySet.exists(present))(present)

      if complete then packageBody(target)
      else
        Def.task {
          val _ = (LocalProject("dixtraCli") / NativeImage.nativeImage).value
          packageBody(target).value
        }
    }

  private def packageBody(target: Option[String]): Def.Initialize[Task[File]] =
    Def.task {
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

      // The outputs of the build: the native executables. They live in the per-platform layouts of sbt under
      // `target/out` of the build and, historically, in `target/native` of the project, so every layout is scanned
      // and the executable of each platform is moved into `dist/` first, in the layout the extension bundles.
      val log              = streams.value.log
      val nativeOutputs =
        val build = (ThisBuild / baseDirectory).value
        val out   = build / "target" / "out"
        val roots = (out / "jvm") +: (out * "*").get().filter(_.isDirectory).map(_ / "jvm")

        (roots.flatMap(root => (root * "scala-*" * "*" * "native").get()) :+ (build / "dixtra-cli" / "target" / "native"))
          .filter(_.isDirectory)
          .distinct

      val dist = (ThisBuild / baseDirectory).value / "dist"

      def moveExecutable(platform: String, name: String, destination: File): Unit =
        nativeOutputs
          .find(output => (output / name).exists())
          .map(_ / name) match {
          case Some(source) =>
            if !destination.exists() || IO.readBytes(source) != IO.readBytes(destination) then
              IO.createDirectory(destination.getParentFile)
              IO.copyFile(source, destination)

            destination.setExecutable(true, false)
            IO.delete(source)
          case None if destination.exists() =>
            ()
          case None =>
            log.warn(
              s"no $platform executable $name among ${nativeOutputs.mkString(", ")}: the extension is packaged without it",
            )
        }

      moveExecutable("Linux", "dixtra", dist / "linux-x64" / "dixtra")
      moveExecutable("Windows", "dixtra.exe", dist / "win32-x64" / "dixtra.exe")

      // The icon of the extension, rendered from the SVG source straight into `bin`, where `vsce` picks it up: the
      // first renderer that works wins. Without a renderer a previously rendered icon is reused, and with none at
      // all the packaging fails. No icon is written to `dist/`.
      val icon = folder / "bin" / "icon.png"

      def renderIcon: Boolean =
        IO.createDirectory(icon.getParentFile)
        val commands =
          Seq(
            Seq(
              "rsvg-convert",
              "-w",
              "256",
              "-h",
              "256",
              (folder / "icon.svg").getPath,
              "-o",
              icon.getPath,
            ),
            Seq(
              "convert",
              "-background",
              "none",
              (folder / "icon.svg").getPath,
              "-resize",
              "256x256",
              icon.getPath,
            ),
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

      // What `vsce` packages must live inside the extension folder: the executables of `dist` are staged into `bin`
      // next to the icon rendered above (nothing from `dist` reaches the repository, it is ignored). The executable
      // of the platform being packaged is required, the others are left out of the package by its ignore file.
      val payload = folder / "bin"

      def stage(platform: String): Option[File] =
        val name     = platforms(platform).executable
        val source   = dist / platform / name
        val staged   = payload / platform / name

        if source.exists() then
          IO.createDirectory(staged.getParentFile)
          if !staged.exists() || IO.readBytes(source) != IO.readBytes(staged) then
            IO.copyFile(source, staged)

          staged.setExecutable(true, false)
          Some(staged)
        else if staged.exists() then 
          Some(staged)
        else 
          None

      val wanted   = target.fold(platforms.keySet.toSeq)(Seq(_))
      val stagedBy = wanted.map(platform => platform -> stage(platform)).toMap

      target.foreach { platform =>
        if stagedBy(platform).isEmpty then
          throw new MessageOnlyException(
            s"no $platform executable ${dist / platform / platforms(platform).executable}: build it with sbt dixtraCli/nativeImage",
          )
      }

      wanted.filter(stagedBy(_).isEmpty).foreach { platform =>
        log.warn(s"no $platform executable ${platforms(platform).executable} in dist: the package has no bundled one")
      }

      // The file listing what the packaging leaves out: one platform of `bin` per target, none for the package of
      // every platform, which bundles them all.
      val ignore = folder / target.fold(".vscodeignore")(platform => s".vscodeignore.$platform")

      if !ignore.exists() then
        throw new MessageOnlyException(s"no ${ignore.getName} in ${folder.getName}: it lists what the packaging skips")

      val extensionName =
        """"name"\s*:\s*"([^"]+)"""".r.findFirstMatchIn(current).map(_.group(1)).getOrElse("dixtra-vscode")
      val vsix = folder / target.fold(s"$extensionName-${version.value}.vsix") { platform =>
        s"$extensionName-$platform-${version.value}.vsix"
      }
      val script = target.fold("package")(platform => platforms(platform).script)

      def build(): Unit =
        IO.delete(vsix)
        npmBuild(folder, log, "install", "--no-fund", "--no-audit")
        npmBuild(folder, log, "run", script)

        if !vsix.exists() then
          throw new MessageOnlyException(s"npm run $script produced no ${vsix.getName}")

      // The inputs of the build: the sources of the extension, its manifest and configuration, the icon source, the
      // ignore file of the target, and the executables and the icon staged above.
      val sources =
        ((folder / "src") ** "*.ts").get() ++
        Seq(folder / "package.json", folder / "tsconfig.json", folder / "icon.svg", ignore) ++
        wanted.flatMap(platform => Seq(dist / platform / platforms(platform).executable, payload / platform / platforms(platform).executable)) ++
        (if icon.exists() then Seq(icon) else Seq.empty)

      val cached =
        FileFunction.cached(streams.value.cacheDirectory / s"vscode-package-${target.getOrElse("all")}", FilesInfo.hash) {
          _ =>
            build()
            Set(vsix)
        }

      val packaged =
        cached(sources.toSet).find(_.exists()).getOrElse {
          build()
          vsix
        }

      // The `.vsix` is copied into `dist/` alongside the executables: the task returns the copy, while the folder one
      // stays as the raw output of vsce.
      val out = dist / packaged.getName

      if !out.exists() || IO.readBytes(packaged) != IO.readBytes(out) then
        IO.createDirectory(dist)
        IO.copyFile(packaged, out)

      out
    }

  private def npmBuild(folder: File, log: Logger, arguments: String*): Unit =
    val npm =
      if sys.props("os.name").toLowerCase.contains("windows") then
        Seq("cmd", "/c", "npm") ++ arguments
      else
        Seq("npm") ++ arguments

    log.info(s"npm ${arguments.mkString(" ")}")

    val exitCode = Process(npm, Some(folder)).!
    if exitCode != 0 then
      throw new MessageOnlyException(s"npm ${arguments.mkString(" ")} failed with exit code $exitCode")

}
