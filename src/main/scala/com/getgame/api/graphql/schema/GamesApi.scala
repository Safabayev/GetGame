package com.getgame.api.graphql.schema

import scala.concurrent.Future
import cats.effect.std.Dispatcher
import sangria.macros.derive.GraphQLField
import sangria.macros.derive.deriveContextObjectType
import sangria.schema.Context
import sangria.schema.ObjectType
import com.getgame.api.graphql._
import com.getgame.domain.Game

class GamesApi[F[_]](implicit dispatcher: Dispatcher[F]) {
  class Queries {
    @GraphQLField
    def games(
        ctx: Context[Ctx[F], Unit]
      ): Future[List[Game]] =
      dispatcher.unsafeToFuture(ctx.ctx.games.fetchAll)
  }

  def queryType: ObjectType[Ctx[F], Unit] =
    deriveContextObjectType[Ctx[F], Queries, Unit](_ => new Queries)

}
