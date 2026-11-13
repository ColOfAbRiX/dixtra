package com.colofabrix.scala.dixtra.bench

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.render.TreeRenderer
import java.nio.charset.StandardCharsets
import org.openjdk.jmh.annotations.*
import scala.meta.*

/**
 * The input of every benchmark: one file of the corpus, formatted with one configuration, and what the pipeline makes
 * of it before the stage under measurement (parsed tree, rendered text, formatted text).
 *
 * The corpus is a frozen copy of some of dixtra's own sources (in `resources/corpus`), so the input stays the same
 * while dixtra changes. Two configurations:
 *   - `check`: the default configuration, which the files already follow: the work of `--check` on a formatted project.
 *   - `reformat`: a narrower `maxColumn`, so that most definitions are rewrapped: the work of formatting a project.
 */
@State(Scope.Benchmark)
class Corpus {

  @Param(Array("ChainRenderer", "TermRenderer", "MethodSpacingNormalizer", "DefinitionAlignment"))
  var file: String = ""

  @Param(Array("check", "reformat"))
  var mode: String = ""

  var source: String      = ""
  var config: Config      = Config.default
  val dialect: Dialect    = SourceDialect.scala
  var tree: Source        = Source(Nil)
  var trees: Vector[Tree] = Vector.empty
  var rendered: String    = ""
  var formatted: String   = ""

  @Setup(Level.Trial)
  def setup(): Unit =
    source = Corpus.read(file)
    config =
      mode match {
        case "check"    => Config.default
        case "reformat" => Config.default.copy(maxColumn = 80)
        case other      => sys.error(s"unknown mode $other")
      }
    tree     = dialect(source).parse[Source].get
    trees    = Corpus.allTrees(tree)
    rendered = new TreeRenderer(source, config).renderSource(tree)
    formatted =
      Dixtra.format(source, config, dialect).fold(e => sys.error(s"$file does not format: ${e.getMessage}"), identity)
    // When the formatting rules change, the frozen corpus may no longer be formatted: reformat its files
    if mode == "check" && formatted != source then sys.error(s"$file is not formatted with the default configuration")

}

object Corpus {

  def read(file: String): String =
    val stream =
      Option(getClass.getResourceAsStream(s"/corpus/$file.scala.txt")).getOrElse(sys.error(s"no corpus file $file"))
    try {
      new String(stream.readAllBytes(), StandardCharsets.UTF_8)
    } finally {
      stream.close()
    }

  // Every node of the tree, in depth-first order
  def allTrees(tree: Tree): Vector[Tree] =
    tree +: tree.children.toVector.flatMap(allTrees)

}
