package com.test.snap

import cats.effect.std.Random
import cats.effect.{IO, IOApp, Ref}
import com.test.snap.domain.Card
import com.test.snap.service.SnapService

object Application extends IOApp.Simple {
  override def run: IO[Unit] =
    for {
      _ <- IO.println("Welcome to Snap!")
      numOfDecks <- IO.println(
        "Please enter the number of decks (minimum number of decks is 1): "
      ) >> IO.readLine.map(_.toInt)
      matchType <- IO.println(
        "Please select whether cards should be matched: on suit, value, or both): "
      ) >> IO.readLine.map(_.toLowerCase)
      numberOfPlayers <- IO.println("Please enter the number of players: ") >> IO.readLine
        .map(_.toInt)
      randomIO <- Random.scalaUtilRandom[IO]
      playerHandsRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
      stacksRef <- Ref.of[IO, Map[Int, List[Card]]](Map.empty)
      service <- SnapService(playerHandsRef, stacksRef, randomIO)
      deck <- service.createAndShuffleDeck(numOfDecks)
      _ <- service.dealCards(numberOfPlayers, deck)
      _ <- service.playRound(numberOfPlayers, matchType)
    } yield ()

}
