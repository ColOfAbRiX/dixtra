package com.colofabrix.scala.dixtra.cli

import cats.effect.*
import java.nio.charset.StandardCharsets
import java.nio.file.*
import scala.concurrent.duration.*

/**
 * UTF-8 file reads and atomic rewrites.
 *
 * A rewrite goes through a temporary file beside the target that is atomically moved over it, keeping the target's
 * permissions, so a file is never left half-written. Symbolic links are resolved so the link itself is preserved. The
 * move is retried a few times when it fails because another process holds the target open, as an antivirus or an IDE
 * indexer briefly does on Windows.
 */
object AtomicFiles {

  /**
   * Read a file as UTF-8 text.
   */
  def read(path: Path): IO[String] =
    IO.blocking(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))

  /**
   * Replace the content of a file atomically.
   */
  def write(path: Path, content: String): IO[Unit] =
    IO
      .blocking(path.toRealPath())
      .flatMap { target =>
        tempFileBeside(target).use { temp =>
          IO.blocking {
            val _ = Files.write(temp, content.getBytes(StandardCharsets.UTF_8))
            copyPermissions(target, temp)
          } >>
          replaceRetrying(temp, target, MoveAttempts)
        }
      }

  private def tempFileBeside(target: Path): Resource[IO, Path] =
    val directory = Option(target.getParent).getOrElse(target.toAbsolutePath.getRoot)
    val prefix    = s".${target.getFileName.toString}."
    Resource.make(IO.blocking(Files.createTempFile(directory, prefix, ".dixtra.tmp"))) { temp =>
      IO.blocking(Files.deleteIfExists(temp)).void
    }

  private def copyPermissions(from: Path, to: Path): Unit =
    if Files.getFileStore(from).supportsFileAttributeView("posix") then
      val _ = Files.setPosixFilePermissions(to, Files.getPosixFilePermissions(from))

  // How many times a move that another process blocks is tried.
  private val MoveAttempts = 5

  // The move, tried again after a growing pause while it fails with an error other than a missing file.
  private def replaceRetrying(source: Path, target: Path, attempts: Int): IO[Unit] =
    IO
      .blocking(replaceAtomically(source, target))
      .handleErrorWith {
        case e: NoSuchFileException =>
          IO.raiseError(e)
        case _: FileSystemException if attempts > 1 =>
          IO.sleep((20 * (MoveAttempts - attempts + 1)).millis) >> replaceRetrying(source, target, attempts - 1)
        case e =>
          IO.raiseError(e)
      }

  private def replaceAtomically(source: Path, target: Path): Unit =
    val _ =
      try {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
      } catch {
        case _: AtomicMoveNotSupportedException =>
          Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
      }

}
