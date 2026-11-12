package com.colofabrix.scala.dixtra.bench

import com.colofabrix.scala.dixtra.normalize.*
import com.colofabrix.scala.dixtra.render.context.*
import com.colofabrix.scala.dixtra.verify.Equivalence
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import scala.meta.*

/**
 * The code that profiling found the most expensive inside the stages of [[PipelineBench]], each on its own:
 *   - `sourceTokens`: building the token index of the source (one tokenization).
 *   - `spans`: `SourceTokens.span` of every tree of the file, which the renderers ask for each node they render.
 *   - `sourceDocs`: `SourceDocs.sourceDoc` of every tree, the verbatim documents of the renderers (spans, plus the
 *     re-normalisation of single-line slices).
 *   - `braceStyle`: the brace-style normalizer on the rendered text (it parses it again and checks its output).
 *   - `statementChecks`: `Equivalence.isEquivalentStat` of every definition and declaration against its own source
 *     text, the renderer's self-check.
 *   - `verifyUnchanged`: `Equivalence.isEquivalentSource` of a text against itself, the `--check` of a formatted file.
 */
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = Array("-Xms2g", "-Xmx2g", "-Xss8m"))
class HotspotBench {

  @Benchmark
  def sourceTokens(c: Corpus): SourceTokens =
    new SourceTokens(c.source)

  @Benchmark
  def spans(c: Corpus, state: HotspotBench.Tokens, bh: Blackhole): Unit =
    c.trees.foreach(t => bh.consume(state.tokens.span(t)))

  @Benchmark
  def sourceDocs(c: Corpus, state: HotspotBench.Tokens, bh: Blackhole): Unit =
    c.trees.foreach(t => bh.consume(state.docs.sourceDoc(t)))

  @Benchmark
  def braceStyle(c: Corpus): String =
    BraceStyleNormalizer.normalize(c.rendered, c.config.braces.style, c.config.maxColumn, c.dialect)

  @Benchmark
  def statementChecks(state: HotspotBench.Tokens, bh: Blackhole): Unit =
    state.statements.foreach { case (stat, text) => bh.consume(Equivalence.isEquivalentStat(stat, text)) }

  @Benchmark
  def verifyUnchanged(c: Corpus): Boolean =
    Equivalence.isEquivalentSource(c.tree, c.source, c.source, c.dialect)

}

object HotspotBench {

  /** The token index and documents of the source, built once, and the statements of the file with their source text. */
  @State(Scope.Benchmark)
  class Tokens {

    var tokens: SourceTokens            = new SourceTokens("")
    var docs: SourceDocs                = new SourceDocs("", tokens)
    var statements: Vector[(Tree, String)] = Vector.empty

    @Setup(Level.Trial)
    def setup(c: Corpus): Unit =
      tokens = new SourceTokens(c.source)
      docs = new SourceDocs(c.source, tokens)
      statements = c.trees.collect { case s: (Defn | Decl) => (s, s.pos.text) }

  }

}
