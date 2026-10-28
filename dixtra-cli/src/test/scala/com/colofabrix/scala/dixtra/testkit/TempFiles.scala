package com.colofabrix.scala.dixtra.testkit

import java.nio.charset.StandardCharsets
import java.nio.file.*

/**
 * Temporary files for the specs that work on the file system.
 */
object TempFiles {

  /**
   * Run `use` on a new temporary directory, deleted afterwards with everything in it, read-only files included.
   */
  def withTempDir[A](use: Path => A): A =
    val dir = Files.createTempDirectory("dixtra-test")
    try {
      use(dir)
    } finally {
      Files
        .walk(dir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(p => { p.toFile.setWritable(true); val _ = Files.deleteIfExists(p) })
    }

  /**
   * Write `content` to `file` in UTF-8, creating its directories.
   */
  def write(file: Path, content: String): Path =
    Option(file.getParent).foreach(Files.createDirectories(_))
    Files.write(file, content.getBytes(StandardCharsets.UTF_8))

  /**
   * The content of `file`, read as UTF-8.
   */
  def read(file: Path): String =
    new String(Files.readAllBytes(file), StandardCharsets.UTF_8)

}
