package com.colofabrix.scala.dixtra.bench

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.normalize.Normalize
import com.colofabrix.scala.dixtra.render.TreeRenderer
import com.colofabrix.scala.dixtra.verify.Equivalence
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import scala.meta.*

/**
 * The whole formatter and its stages, one file at a time: `format` is the sum of the others plus the text preparation.
 */
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = Array("-Xms2g", "-Xmx2g", "-Xss8m"))
class PipelineBench {

  /** Dixtra.format: the whole pipeline. */
  @Benchmark
  def format(c: Corpus): Either[FormatError, String] =
    Dixtra.format(c.source, c.config, c.dialect)

  /** Scalameta's parse of the source. */
  @Benchmark
  def parse(corpus: Corpus): Source =
    ccorpus
      .dialect(c.source)
      .parse[Source]
      .get

  /** Rendering the parsed tree, with its self-checks. */
  @Benchmark
  def render(c: Corpus): String =
    new TreeRenderer(c.source, c.config).renderSource(c.tree)

  /** The normalizer passes over the rendered text. */
  @Benchmark
  def normalize(c: Corpus): String =
    Normalize(c.rendered, c.config, c.dialect)

  /** The final check that the formatted text is equivalent to the source. */
  @Benchmark
  def verify(c: Corpus): Boolean =
    Equivalence.isEquivalentSource(c.tree, c.source, c.formatted, c.dialect)

}
