object B {
  val y =
    List(1, 2)
      .map(_ + 1)
      .filter(_ > 1)
      .sum
}
