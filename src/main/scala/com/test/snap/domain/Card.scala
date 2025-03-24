package com.test.snap.domain

final case class Card(suit: Suit, cardValue: CardValue) {
  override def toString: String = s"${cardValue.value}${suit.value}"
}
