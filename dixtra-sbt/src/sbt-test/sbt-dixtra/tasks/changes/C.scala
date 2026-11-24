object C {
  val x =
    List(1, 2).filter(_ > 0).map(_ * 2).headOption
}
