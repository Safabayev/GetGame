package com.getgame.api.graphql.schema

import cats.effect.kernel.Sync
import cats.implicits._
import scala.concurrent.Future
import cats.effect.std.Dispatcher
import sangria.macros.derive.GraphQLField
import sangria.macros.derive.deriveContextObjectType
import sangria.schema.Context
import sangria.schema.ObjectType
import com.getgame.api.graphql._
import com.getgame.domain.Game

import java.time.ZonedDateTime
import java.util.UUID

class GamesApi[F[_]: Sync](implicit dispatcher: Dispatcher[F]) {
  class Queries {
    @GraphQLField
    def games(
        ctx: Context[Ctx[F], Unit],
        genre: Option[String]
      ): Future[List[Game]] =
      dispatcher.unsafeToFuture(ctx.ctx.games.fetchAll(genre))
  }

  class Mutations {
    @GraphQLField
    def create(
        ctx: Context[Ctx[F], Unit],
        title: String,
        genre: String,
        platform: String,
        developer: String
      ): Future[UUID] = {
      val createTask = for {
        id <- Sync[F].delay(UUID.randomUUID())
        now <- Sync[F].delay(ZonedDateTime.now)
        game = Game(id, title, genre, platform, developer, now)
        _ <- ctx.ctx.games.createGame(game)
      } yield id
      dispatcher.unsafeToFuture(createTask)
    }
  }
  def queryType: ObjectType[Ctx[F], Unit] =
    deriveContextObjectType[Ctx[F], Queries, Unit](_ => new Queries)
  def mutationType: ObjectType[Ctx[F], Unit] =
    deriveContextObjectType[Ctx[F], Mutations, Unit](_ => new Mutations)

}
