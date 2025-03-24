package com.test.snap.service

import cats.effect.std.Random
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Ref}
import cats.implicits.catsSyntaxApplicativeId
import com.test.snap.domain.{Ace, Card, CardValue, Clubs, Diamond, Five, Heart, King, Spade, Suit, Three, Two}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

class SnapServiceSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  implicit val random: Random[IO] = Random.scalaUtilRandom[IO].unsafeRunSync()

  "SnapService" - {
    "must create and shuffle at least one deck" in {
      val program = for {
        playerHandsRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        stacksRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        service <- new MainSnapService[IO](playerHandsRef, stacksRef).pure[IO]
        result <- service.createAndShuffleDeck(1)
      } yield result

      program.asserting { res =>
        val resultSuits = res.map(_.suit)
        val resultCardValues = res.map(_.cardValue)

        resultSuits.size mustBe 52
        resultSuits must contain atLeastOneElementOf Suit.suits
        resultCardValues.size mustBe 52
        resultCardValues must contain atLeastOneElementOf CardValue.values

      }
    }

    "must create and shuffle at least three decks" in {
      val program = for {
        playerHandsRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        stacksRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        service <- new MainSnapService[IO](playerHandsRef, stacksRef).pure[IO]
        result <- service.createAndShuffleDeck(3)
      } yield result

      program.asserting { res =>
        val resultSuits = res.map(_.suit)
        val resultCardValues = res.map(_.cardValue)

        resultSuits.size mustBe 156
        resultSuits must contain atLeastOneElementOf Suit.suits
        resultCardValues.size mustBe 156
        resultCardValues must contain atLeastOneElementOf CardValue.values
      }
    }

    "cards must be distributed equally to players discarding any cards left over" in {
      val deckOfCards = List(
        Card(Heart, Two),
        Card(Diamond, Ace),
        Card(Spade, Three),
        Card(Clubs, Five),
        Card(Heart, King)
      )
      val program = for {
        playerHandsRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        stacksRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        service <- new MainSnapService[IO](playerHandsRef, stacksRef).pure[IO]
        result <- service.dealCards(numberOfPlayers = 2, deckOfCards)
        newPlayerHands <- playerHandsRef.get
      } yield (result, newPlayerHands)

      program.asserting {
        case (result, newPlayerHands) =>
          result.head._2.size mustBe 2
          result.tail.head._2.size mustBe 2
          newPlayerHands.values.flatten.size mustBe 4
      }
    }

    "must continuously play round until there is a winner, winner must have all cards in the stack" in {
      val program = for {
        playerHandsRef <- Ref.of[IO, Map[Int, List[Card]]](
          Map(
            0 -> List(Card(Heart, Two), Card(Heart, Two), Card(Heart, Two)),
            1 -> List(Card(Heart, Two), Card(Heart, Two), Card(Heart, Two))
          )
        )
        stacksRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
        service <- new MainSnapService[IO](playerHandsRef, stacksRef).pure[IO]
        _ <- service.playRound(players = 2, matchType = "both")
        result <- playerHandsRef.get
      } yield result

      program asserting { playerHands =>
        val playerOneHands = playerHands.get(0).map(_.size)
        val playerTwoHands = playerHands.get(1).map(_.size)

        playerOneHands must (equal(Some(6)) or equal(Some(0)))
        playerTwoHands must (equal(Some(6)) or equal(Some(0)))

      }
    }
  }

}
