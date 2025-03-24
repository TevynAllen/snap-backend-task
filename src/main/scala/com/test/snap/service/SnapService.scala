package com.test.snap.service

import cats.effect.Ref
import cats.effect.kernel.Async
import cats.effect.std.{Console, Random}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxFlatMapOps, toFlatMapOps, toFoldableOps}
import cats.syntax.all.toFunctorOps
import com.test.snap.domain.{Card, CardValue, Suit}

trait SnapService[F[_]] {
  def createAndShuffleDeck(numberOfDecks: Int): F[List[Card]]
  def dealCards(numberOfPlayers: Int, deck: List[Card]): F[Map[Int, List[Card]]]
  def playRound(players: Int, matchType: String): F[Unit]
}

class MainSnapService[F[_]: Async: Console](
  playerHandsRef: Ref[F, Map[Int, List[Card]]],
  stacksRef: Ref[F, Map[Int, List[Card]]]
)(implicit random: Random[F])
    extends SnapService[F] {

  override def createAndShuffleDeck(numberOfDecks: Int): F[List[Card]] = {
    random.shuffleList(for {
      _ <- (1 to numberOfDecks).toList
      suit <- Suit.suits
      cardValue <- CardValue.values
    } yield Card(suit, cardValue))
  }

  override def dealCards(numberOfPlayers: Int,
                         deck: List[Card]): F[Map[Int, List[Card]]] = {
    val playersNumber = if (numberOfPlayers <= 1) 2 else numberOfPlayers
    Async[F]
      .pure {
        val trimmedDeck = deck.take((deck.size / playersNumber) * playersNumber)
        trimmedDeck
          .grouped(playersNumber)
          .toList
          .transpose
          .zipWithIndex
          .map {
            case (cards, index) => index -> cards
          }
          .toMap
      }
      .flatTap(hands => playerHandsRef.set(hands))
  }

  def playRound(players: Int, matchType: String): F[Unit] = {
    playerHandsRef.get.flatMap { playerHands =>
      val activePlayers = playerHands.filter(_._2.nonEmpty).keys.toSet
      if (activePlayers.isEmpty)
        Console[F].println("Game Over! No card left to play.")
      else if (activePlayers.size == 1)
        Console[F].println(s"Winner: Player ${activePlayers.head}")
      else {
        activePlayers.toList.traverse_ { player =>
          for {
            hands <- playerHandsRef.get
            stacks <- stacksRef.get
            _ <- hands.get(player) match {
              case Some(card :: remaining) =>
                val updatedHands = hands.updated(player, remaining)
                val updatedStacks = stacks.updated(
                  player,
                  stacks.getOrElse(player, List()) :+ card
                )
                Console[F].println(s"Player $player plays: $card") >>
                  playerHandsRef.set(updatedHands) >>
                  stacksRef.set(updatedStacks) >>
                  checkSnap(matchType, updatedStacks)
              case _ => Async[F].unit
            }
          } yield ()
        } >> playRound(players, matchType)
      }
    }
  }

  private def checkSnap(matchType: String,
                        stacks: Map[Int, List[Card]],
  ): F[Unit] = {

    val matchingStacks: Map[Int, Card] = stacks
      .collect {
        case (player, stack) if stack.nonEmpty =>
          player -> stack.last
      }
      .groupBy {
        case (_, card) =>
          matchType match {
            case "suit"  => card.suit.value
            case "value" => card.cardValue.value
            case "both"  => (card.cardValue.value, card.suit.value)
          }
      }
      .collect {
        case (_, matching) if matching.size > 1 => matching
      }
      .flatten
      .toMap

    if (matchingStacks.nonEmpty) {
      val randomSnapper =
        matchingStacks.keys.toList(util.Random.nextInt(matchingStacks.size))
      Console[F].println(s"Player $randomSnapper calls SNAP!") >>
        playerHandsRef.update { hands =>
          hands.updated(
            randomSnapper,
            hands(randomSnapper) ++ stacks.values.toList.flatten
          )
        } >>
        stacksRef.update(_ => Map.empty)
    } else Async[F].unit
  }

}

object SnapService {
  def apply[F[_]: Async: Console](playerHandsRef: Ref[F, Map[Int, List[Card]]],
                                  stacksRef: Ref[F, Map[Int, List[Card]]],
                                  random: Random[F]): F[MainSnapService[F]] = {
    implicit val ran: Random[F] = random
    new MainSnapService[F](playerHandsRef, stacksRef).pure[F]
  }
}
