package com.getgame.scheduler

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent.duration.Duration

import cats.effect.Async
import cats.effect.kernel.Sync
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import org.typelevel.log4cats.Logger

import com.getgame.domain.Game
import com.getgame.domain.freeToGameAPI.GameResp
import com.getgame.integration.FreeGameApiClient
import com.getgame.repos.GamesRepository

class GetGamesScheduler[F[_]: Async: Logger](interval: Duration) {
  private def getGamesFromApi(repo: GamesRepository[F]): F[Unit] = {

    val freeGameApiClient = new FreeGameApiClient()
    def gameRespToGame(gameResp: GameResp): F[Game] =
      for {
        id <- Sync[F].delay(UUID.randomUUID())
        createdAt <- Sync[F].delay(ZonedDateTime.now())
        game = Game(
          id = id,
          title = gameResp.title,
          genre = gameResp.genre,
          platform = gameResp.platform,
          developer = gameResp.developer,
          createdAt = createdAt,
        )
      } yield game
    for {
      _ <- Logger[F].info("GET GAMES FROM API")
      games <- freeGameApiClient.getGames.traverse(gameRespToGame)
      _ <- repo.createGames(games)
    } yield ()
  }

  def scheduleTask(repo: GamesRepository[F]): F[Unit] =
    for {
      _ <- Logger[F].info("Starting Get Game Scheduler")
      _ <- Sync[F].sleep(interval)
      _ <- getGamesFromApi(repo)
      _ <- scheduleTask(repo)
    } yield ()
}
