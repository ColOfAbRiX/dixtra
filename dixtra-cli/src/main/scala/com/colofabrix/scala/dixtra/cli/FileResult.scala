package com.colofabrix.scala.dixtra.cli

import com.colofabrix.scala.dixtra.FormatError
import java.nio.file.Path

/**
 * Outcome of processing a single Scala source file.
 */
enum FileResult {

  /**
   * The file was already formatted; nothing changed.
   */
  case Unchanged(path: Path)

  /**
   * The file was reformatted and rewritten (format mode).
   */
  case Formatted(path: Path)

  /**
   * The file would change but was left untouched (check mode).
   */
  case WouldFormat(path: Path)

  /**
   * The file was left untouched because it is not formatted, for `reason` (Scala 2 sources). It is not a failure.
   */
  case Skipped(path: Path, reason: String)

  /**
   * The file could not be parsed or read.
   */
  case Failed(path: Path, message: String)

}

object FileResult {

  /**
   * A failure for `path` caused by `error`, described by its message.
   */
  def failed(path: Path, error: Throwable): FileResult =
    Failed(path, FormatError.describe(error))

}
