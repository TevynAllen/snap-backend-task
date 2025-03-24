package com.test.snap.domain

sealed trait CardValue {
  def value: String
}

case object Two extends CardValue {
  override def value: String = "2"
}
case object Three extends CardValue {
  override def value: String = "3"
}
case object Four extends CardValue {
  override def value: String = "4"
}
case object Five extends CardValue {
  override def value: String = "5"
}
case object Six extends CardValue {
  override def value: String = "6"
}
case object Seven extends CardValue {
  override def value: String = "7"
}
case object Eight extends CardValue {
  override def value: String = "8"
}
case object Nine extends CardValue {
  override def value: String = "9"
}
case object Ten extends CardValue {
  override def value: String = "10"
}
case object Jack extends CardValue {
  override def value: String = "J"
}
case object Queen extends CardValue {
  override def value: String = "Q"
}
case object King extends CardValue {
  override def value: String = "K"
}
case object Ace extends CardValue {
  override def value: String = "A"
}

object CardValue {
  val values: List[CardValue] = List(
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine,
    Ten,
    Jack,
    Queen,
    King,
    Ace
  )
}
