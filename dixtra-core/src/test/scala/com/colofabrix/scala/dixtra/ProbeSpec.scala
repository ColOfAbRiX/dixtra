package com.colofabrix.scala.dixtra

import org.scalatest.wordspec.AnyWordSpec

// TEMPORARY: prints two formatting passes of candidate reductions. To be deleted.
class ProbeSpec extends AnyWordSpec {

  private val snippets: Vector[(String, String)] =
    Vector(
      "e1-else-block-if" ->
      """object A {
          |  def f(a: Boolean, b: Boolean) = {
          |    if (a) List.empty
          |    else {
          |      if (b) {
          |        1 :: Nil
          |      } else {
          |        Nil
          |      }
          |    }
          |  }
          |}
          |""".stripMargin,
      "e2-leading-comment-blank" ->
      """object A {
          |  def f(x: Int) = {
          |    // a comment
          |    // more comment
          |
          |    val y = x
          |    y
          |  }
          |}
          |""".stripMargin,
      "e3-for-new-multiline" ->
      """object A {
          |  def f =
          |    for {
          |      meter <- get("Test Meter")
          |      interpreter = new ServerInterpreter[Any, IO](
          |        serverEndpoints = _ => List(endpoint),
          |        deleteFile = _ => IO.pure(())
          |      )
          |      _ <- interpreter(request)
          |    } yield meter
          |}
          |""".stripMargin,
      "e4-for-comment-first" ->
      """object A {
          |  def f =
          |    for {
          |      // comment
          |      description <- c.downField("description")
          |      content <- c.downField("content")
          |    } yield content
          |}
          |""".stripMargin,
      "e5-case-paren-infix" ->
      """object A {
          |  def f =
          |    g(
          |      (tpe match {
          |        case r: OpenapiSchemaRef => r.stripped
          |        case _ => vn
          |      }) + "Validator",
          |      nb
          |    )
          |}
          |""".stripMargin,
      "e6-defs-after-comment" ->
      """class A {
          |
          |  @volatile private var subscribed: Boolean = false
          |  def wasSubscribed: Boolean = subscribed
          |
          |  // a comment
          |  @volatile private var lazily: Boolean = false
          |  def markLazily(): Unit = lazily = true
          |  def isLazily: Boolean = lazily
          |
          |}
          |""".stripMargin,
      "e7-braceless-class-long" ->
      """class NettySyncRequestTimeoutTests(eventLoopGroup: EventLoopGroup, backend: WebSocketStreamBackend[IO, Fs2Streams[IO]]):
          |  val logger = 1
          |""".stripMargin,
      "e8-if-block-pure" ->
      """object A {
          |  def f = {
          |    if (a) {
          |      // fast track: a comment
          |
          |      (in: Stream[F]) =>
          |        val x = in
          |        x
          |    }.pure[F]
          |    else {
          |      g
          |    }
          |  }
          |}
          |""".stripMargin,
      "e9-for-infix-lambda" ->
      """object A {
          |  def f =
          |    for {
          |      promise <- Promise.make[Nothing, Unit]
          |      _ <- stream.runForeach { chunk =>
          |        val buffer = fn(chunk)
          |        buffer
          |      } onExit {
          |        case _ => ZIO.unit
          |      } forkDaemon
          |    } yield promise
          |}
          |""".stripMargin,
    )

  "probe" should {
    snippets.foreach { case (name, source) =>
      name in {
        val pass1 = Dixtra.format(source)
        val pass2 = pass1.flatMap(Dixtra.format)
        val shown =
          (pass1, pass2) match {
            case (Right(a), Right(b)) if a == b => s"STABLE\n$a"
            case (Right(a), Right(b))           => s"UNSTABLE\n--- pass1\n$a--- pass2\n$b"
            case (Left(e), _)                   => "ERROR pass1 " + e.getMessage
            case (_, Left(e))                   => "ERROR pass2 " + e.getMessage
          }
        info(s"[$name] $shown")
      }
    }
  }

}
