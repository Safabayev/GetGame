package com.getgame

import java.time.ZonedDateTime
import java.util.UUID

import scala.annotation.unused
import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.Async
import cats.effect.Resource
import cats.effect.Sync
import cats.effect.std.Console
import cats.effect.std.Dispatcher
import cats.implicits.catsSyntaxApplicativeError
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import doobie.HC
import doobie.util.transactor.Transactor
import io.getquill.CompositeNamingStrategy2
import io.getquill.PluralizedTableNames
import io.getquill.SnakeCase
import io.getquill.doobie.DoobieContext
import org.typelevel.log4cats.Logger
import pureconfig.generic.auto.exportReader
import sangria.execution.deferred.DeferredResolver
import sangria.schema.Schema

import com.getgame.api.graphql.GraphQL
import com.getgame.api.graphql.SangriaGraphQL
import com.getgame.api.graphql.schema.GamesApi
import com.getgame.domain.Game
import com.getgame.domain.freeToGameAPI.GameResp
import com.getgame.integration.FreeGameApiClient
import com.getgame.repos.GamesRepository
import com.getgame.repos.Repositories
import com.getgame.utils.Migrations

case class Environment[F[_]](
    config: Config,
    graphQL: GraphQL[F],
  ) {}

object Environment {
  private val myNamingStrategy: CompositeNamingStrategy2[SnakeCase, PluralizedTableNames] =
    CompositeNamingStrategy2(SnakeCase, PluralizedTableNames)
  type DBContext = DoobieContext.Postgres[CompositeNamingStrategy2[SnakeCase, PluralizedTableNames]]

  @unused
  private def makeTransactor[F[_]: Async](dbConfig: Config.DataBaseConfig): Transactor[F] =
    Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.database}",
      s"${dbConfig.user}",
      s"${dbConfig.password}",
    )

  def graphQL[F[_]: Async: Logger: Dispatcher](
      repositories: Repositories[F]
    ): GraphQL[F] =
    SangriaGraphQL[F](
      Schema(
        query = new GamesApi[F].queryType,
        mutation = new GamesApi[F].mutationType.some,
      ),
      DeferredResolver.empty,
      repositories.pure[F],
      global,
    )
  private def getGamesFromApi[F[_]: Async: Logger](repo: GamesRepository[F]): F[Unit] = {
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
      games <- freeGameApiClient.getGames.traverse(gameRespToGame)
      _ <- repo.createGames(games)
    } yield ()
  }
  def make[F[_]: Async: Console: Logger]: Resource[F, Environment[F]] =
    for {
      config <- Resource.eval(ConfigLoader.load[F, Config])
      _ <- Resource.eval(Migrations.run[F](config.migrations))
      implicit0(dispatcher: Dispatcher[F]) <- Dispatcher.parallel[F]
      implicit0(context: DBContext) = new DoobieContext.Postgres(myNamingStrategy)
      implicit0(xa: Transactor[F]) = makeTransactor(config.database)
      repos = Repositories.make[F]
      _ <- Resource.eval(getGamesFromApi[F](repos.games))
      graphql = graphQL[F](repos)
    } yield Environment[F](config, graphql)
}
