package com.test.snap.domain

sealed trait Suit {
  def value: String
}

case object Heart extends Suit {
  override def value: String = "♥"
}

case object Diamond extends Suit {
  override def value: String = "♦"
}

case object Clubs extends Suit {
  override def value: String = "♣"
}

case object Spade extends Suit {
  override def value: String = "♠"
}

object Suit {
  val suits: List[Suit] = List(Heart, Diamond, Clubs, Spade)
}
