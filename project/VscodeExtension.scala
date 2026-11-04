import sbt.*
import sbt.Keys.*
import scala.sys.process.*

/**
 * Packages the VS Code extension of `dixtra-vscode/` into a `.vsix` with `vsce`.
 *
 * The extension formats the document of the editor with dixtra on Format Document, and npm builds it in the folder of
 * the project. `vscodePackage` installs the npm dependencies, compiles the extension and packages it into a `.vsix`,
 * again when its sources change, and copies it into the `dist/` folder of the build, next to the executables it
 * bundles.
 */
object VsCodeExtension {

  // The build task of the VS Code extension in `dixtra-vscode/`
  @transient
  val vscodePackage =
    taskKey[File]("Package the VS Code extension into a .vsix, in the output directory of the build")

  /**
   * The tasks of the extension project: `vscodePackage`, which packages the extension of `dixtra-vscode/`.
   */
  def settings: Seq[Setting[?]] =
    Seq(
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

        // The outputs of the build in `dist/`: the executables bundled with the extension. The native builds
        // of `dixtraCli` write into the shared drive: the one of the current platform first and the ones of the other
        // platforms alongside, in the per-platform layouts of sbt, so the extension moves every executable it finds
        // into `dist/` and warns about the ones missing from both the build and the output.
        val log              = streams.value.log
        val nativeExecutable = (LocalProject("dixtraCli") / NativeImage.nativeImage).value
        val nativeOutputs =
          val out   = (ThisBuild / baseDirectory).value / "target" / "out"
          val roots = (out / "jvm") +: (out * "*").get().filter(_.isDirectory).map(_ / "jvm")

          (nativeExecutable.getParentFile +: roots.flatMap(root => (root * "scala-*" * "*" * "native").get()))
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

        // What `vsce` packages must live inside the extension folder: the Windows executable of `dist` is staged into
        // `bin` before packaging (nothing from `dist` reaches the repository, it is ignored), next to the icon
        // rendered above.
        val payload = folder / "bin"
        val windows = dist / "win32-x64" / "dixtra.exe"

        if windows.exists() then
          IO.createDirectory(payload / "win32-x64")
          IO.copyFile(windows, payload / "win32-x64" / "dixtra.exe")

        // The inputs of the build: the sources of the extension, its manifest and configuration, the icon source, and
        // the executables and the icon staged above. The icon changes only when `icon.svg` does.
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

        val vsix = produced.getOrElse(throw new MessageOnlyException("The build of the extension produced no .vsix"))

        // The `.vsix` is copied into `dist/` alongside the executables: the task returns the copy, while
        // the folder one stays as the raw output of vsce.
        val packaged = dist / vsix.getName

        if !packaged.exists() || IO.readBytes(vsix) != IO.readBytes(packaged) then
          IO.createDirectory(dist)
          IO.copyFile(vsix, packaged)

        packaged
      },
    )

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
