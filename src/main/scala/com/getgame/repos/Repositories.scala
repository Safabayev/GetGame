package com.getgame.repos

import cats.effect.MonadCancelThrow
import doobie.util.transactor.Transactor
import com.getgame.Environment.DBContext
import org.typelevel.log4cats.Logger

case class Repositories[F[_]](
    games: GamesRepository[F]
  )
object Repositories {
  def make[F[_]: MonadCancelThrow: Logger](implicit ctx: DBContext, xa: Transactor[F]): Repositories[F] =
    Repositories[F](
      games = GamesRepository.make[F]
    )
}
