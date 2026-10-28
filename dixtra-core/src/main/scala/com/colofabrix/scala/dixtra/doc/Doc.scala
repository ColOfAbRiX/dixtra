package com.colofabrix.scala.dixtra.doc

/**
 * A minimal document intermediate representation tuned for the All-or-Nothing (AON) style.
 *
 * Unlike a Wadler/Paiges pretty-printer, this IR has no automatic flat-vs-broken selection: the explode-vs-inline
 * decision is made upstream by the renderers and encoded directly as hard `Line`s. The renderer therefore only realizes
 * a fully-decided layout — it never searches. This keeps the AON rules (which must explode even when a construct fits)
 * a 1:1 match to the IR.
 */
enum Doc {

  /**
   * The empty document.
   */
  case Empty

  /**
   * A literal chunk of text that contains no newlines.
   */
  case Text(value: String)

  /**
   * Sequential composition of two documents.
   */
  case Concat(left: Doc, right: Doc)

  /**
   * A hard line break; the next content starts at the current indent.
   */
  case Line

  /**
   * Increase the indentation of the nested document by `spaces`.
   */
  case Nest(spaces: Int, doc: Doc)

  /**
   * A document that must start on its own line even when it has no line break (e.g. a block holding a single
   * definition, which is not valid after `=` on the same line). It renders as `doc` but never counts as single-line.
   */
  case Standalone(doc: Doc)

  /**
   * A raw source slice emitted verbatim by the safety net. May contain newlines;
   * the renderer shifts continuation lines so that their indentation relative to `sourceIndent` (the indentation of the
   * source line where the slice starts) is kept relative to the current indent. Lines listed in `frozenLines` (0-based,
   * relative to the slice) begin inside a multiline string literal and are emitted untouched, because their leading
   * whitespace is string content.
   */
  case Verbatim(value: String, sourceIndent: Int, frozenLines: Set[Int])

}

object Doc {

  /**
   * Concatenate a sequence of documents.
   */
  def concat(docs: Iterable[Doc]): Doc =
    docs.foldLeft(Doc.Empty: Doc)(_ ++ _)

  /**
   * Join documents with a separator document between each pair.
   */
  def intercalate(separator: Doc, docs: Iterable[Doc]): Doc =
    docs
      .iterator
      .zipWithIndex
      .foldLeft(Doc.Empty: Doc) { case (acc, (d, i)) =>
        if i == 0 then acc ++ d else acc ++ separator ++ d
      }

  /**
   * `content` in braces: `{`, the content one level in on the lines below, and `}` on a line of its own.
   */
  def braced(content: Doc): Doc =
    Doc.text("{") ++ Doc.Nest(2, Doc.Line ++ content) ++ Doc.Line ++ Doc.text("}")

  extension (self: Doc) {

    /**
     * Sequentially compose this document with another.
     */
    def ++(other: Doc): Doc =
      (self, other) match {
        case (Doc.Empty, r) => r
        case (l, Doc.Empty) => l
        case (l, r)         => Doc.Concat(l, r)
      }

  }

  /**
   * Lift a string into a text document.
   */
  def text(value: String): Doc =
    Doc.Text(value)

}
