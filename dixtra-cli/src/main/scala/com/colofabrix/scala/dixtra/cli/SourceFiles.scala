package com.colofabrix.scala.dixtra.cli

import cats.effect.IO
import cats.syntax.all.*
import com.colofabrix.scala.dixtra.PathsConfig
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

/**
 * Discovers the Scala sources named by the CLI paths: a file is taken as is and a directory is walked recursively for
 * every `.scala` and `.sbt` file, listed in path order so that the result does not depend on the file system. Hidden
 * directories and `target` directories inside it are skipped, and the files are selected by include and exclude glob
 * patterns relative to it.
 */
object SourceFiles {

  /**
   * The source files of every input path, in input order, keeping inside each walked directory only the files selected
   * by `filter`. A file given explicitly is always kept. A path that cannot be expanded (for example an unreadable
   * directory) yields a failure instead.
   */
  def discover(paths: List[Path], filter: PathsConfig): IO[List[Either[FileResult, Path]]] =
    val selection = Selection(filter.included.map(globRegex), filter.excluded.map(globRegex))
    paths.flatTraverse(expand(_, selection))

  /**
   * The directory `path` is (absolute), or else the one it is in.
   */
  private[cli] def directoryOf(path: Path): Option[Path] =
    val absolute = path.toAbsolutePath.normalize
    if Files.isDirectory(absolute) then Some(absolute) else Option(absolute.getParent)

  // The compiled include and exclude patterns.
  private final case class Selection(include: List[Regex], exclude: List[Regex])

  private def expand(path: Path, selection: Selection): IO[List[Either[FileResult, Path]]] =
    IO
      .blocking(Files.isDirectory(path))
      .flatMap { isDirectory =>
        if isDirectory then walkScalaSources(path, selection).map(_.map(_.asRight[FileResult]))
        else IO.pure(List(path.asRight[FileResult]))
      }
      .handleError(e => List(FileResult.failed(path, e).asLeft[Path]))

  // The skipped directories are not entered: a `target` directory can hold thousands of files.
  private def walkScalaSources(directory: Path, selection: Selection): IO[List[Path]] =
    IO.blocking {
      val found = ListBuffer.empty[Path]
      val visitor =
        new SimpleFileVisitor[Path] {
          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
            if dir != directory && isSkippedDirectory(dir) then FileVisitResult.SKIP_SUBTREE
            else FileVisitResult.CONTINUE

          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
            val isSource = attrs.isRegularFile && isScalaSource(file)
            if isSource && isSelected(directory, file, selection) then
              found += file
            FileVisitResult.CONTINUE
        }
      val _ = Files.walkFileTree(directory, visitor)
      found.toList.sorted
    }

  // A file whose path relative to the walked directory matches an include pattern, while neither it nor a directory
  // above it matches an exclude one.
  private def isSelected(directory: Path, file: Path, selection: Selection): Boolean =
    val segments =
      directory
        .relativize(file)
        .iterator()
        .asScala
        .map(_.toString)
        .toList

    val prefixes       = segments.indices.map(i => segments.take(i + 1).mkString("/"))
    val relative       = segments.mkString("/")
    val matchesInclude = selection.include.exists(_.matches(relative))
    val excluded       = selection.exclude.exists(pattern => prefixes.exists(pattern.matches))

    matchesInclude && !excluded

  // A glob as a regular expression: `**/` matches any number of directories, `**` anything, `*` anything within a
  // segment, `?` one character within one.
  private[cli] def globRegex(glob: String): Regex =
    @tailrec
    def loop(rest: List[Char], acc: StringBuilder): String =
      rest match {
        case '*' :: '*' :: '/' :: tail => loop(tail, acc.append("(?:.*/)?"))
        case '*' :: '*' :: tail        => loop(tail, acc.append(".*"))
        case '*' :: tail               => loop(tail, acc.append("[^/]*"))
        case '?' :: tail               => loop(tail, acc.append("[^/]"))
        case c :: tail                 => loop(tail, acc.append(Regex.quote(c.toString)))
        case Nil                       => acc.toString
      }
    loop(glob.replace('\\', '/').toList, new StringBuilder).r

  private def isScalaSource(path: Path): Boolean =
    val name = path.getFileName.toString
    name.endsWith(".scala") || name.endsWith(".sbt")

  // A hidden directory (`.git`, `.history`, `.metals`, ...) or a `target` build directory below the walked directory:
  // these hold tool state and build output, not sources.
  private def isSkippedDirectory(dir: Path): Boolean =
    val name = Option(dir.getFileName).fold("")(_.toString)
    name.startsWith(".") || name == "target"

}
