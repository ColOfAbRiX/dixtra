package com.colofabrix.scala.dixtra.cli

import cats.effect.IO
import com.colofabrix.scala.dixtra.FormatError
import java.nio.file.*
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.*

/**
 * The files changed in git since a ref, for `--changed-since`: the files of the working tree that differ from the ref,
 * whether committed since or not, and the untracked files that git does not ignore. Deleted files are left out.
 */
object GitChanges {

  /**
   * Whether a file changed since `ref` in the git repository that contains `path`, or what went wrong.
   */
  def selector(ref: String, path: Path): IO[Either[String, Path => Boolean]] =
    since(ref, path).map(_.map(changed => (file: Path) => realPath(file).exists(changed)))

  // The real paths (see [[Path.toRealPath]]) of the files changed since `ref`.
  private def since(ref: String, path: Path): IO[Either[String, Set[Path]]] =
    IO.blocking {
      for
        start     <- SourceFiles.directoryOf(path).toRight(s"no directory for ${path.toString}")
        top       <- toplevel(start)
        changed   <- git(top, "diff", "--name-only", ref, "--")
        untracked <- git(top, "ls-files", "--others", "--exclude-standard")
      yield
        (changed ++ untracked).flatMap(file => realPath(top.resolve(file))).toSet
    }

  // The root of the working tree that contains `dir`.
  private def toplevel(dir: Path): Either[String, Path] =
    git(dir, "rev-parse", "--show-toplevel").map(lines => Paths.get(lines.mkString.trim))

  private def realPath(path: Path): Option[Path] =
    Try(path.toRealPath()).toOption

  // The output lines of a git command run in `dir`, or its error. Paths are printed as they are (`core.quotePath`), not
  // quoted with octal escapes.
  private def git(dir: Path, args: String*): Either[String, List[String]] =
    val command = s"git ${args.mkString(" ")}"
    val run =
      Try {
        val arguments = "git" :: "-c" :: "core.quotePath=false" :: args.toList
        val process =
          new ProcessBuilder(arguments.asJava)
            .directory(dir.toFile)
            .redirectErrorStream(true)
            .start()
        val output = Using.resource(Source.fromInputStream(process.getInputStream, "UTF-8"))(_.getLines().toList)
        (process.waitFor(), output)
      }
    run
      .toEither
      .left
      .map(e => s"$command: ${FormatError.describe(e)}")
      .flatMap {
        case (0, output) => Right(output)
        case (_, output) => Left(s"$command: ${output.mkString(" ").trim}")
      }

}
