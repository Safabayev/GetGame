package com.getgame.repos

import cats.effect.kernel.MonadCancelThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

import com.getgame.Environment.DBContext
import com.getgame.domain.Game

trait GamesRepository[F[_]] {
  def createGames(games: List[Game]): F[Unit]
  def createGame(games: Game): F[Unit]
  def fetchAll: F[List[Game]]
}
object GamesRepository {
  def make[F[_]: MonadCancelThrow: Logger](
      implicit
      ctx: DBContext,
      xa: Transactor[F],
    ): GamesRepository[F] =
    new GamesRepository[F] {
      import ctx._

      override def fetchAll: F[List[Game]] =
        run(query[Game]).transact(xa)

      override def createGames(games: List[Game]): F[Unit] =
        run(
          liftQuery(games)
            .foreach { game =>
              query[Game]
                .insertValue(game)
                .onConflictUpdate(_.title)(
                  _.genre -> _.genre,
                  _.platform -> _.platform,
                  _.developer -> _.developer,
                )
            }
        ).transact(xa).void

      override def createGame(game: Game): F[Unit] =
        run {
          query[Game]
            .insertValue(lift(game))
            .onConflictIgnore
        }
          .transact(xa)
          .void
    }
}
