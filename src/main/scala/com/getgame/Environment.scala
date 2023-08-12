package com.getgame

import scala.annotation.unused
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import cats.effect.Async
import cats.effect.Resource
import cats.effect.implicits.genSpawnOps
import cats.effect.std.Console
import cats.effect.std.Dispatcher
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.catsSyntaxOptionId
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
import com.getgame.repos.GamesRepository
import com.getgame.repos.Repositories
import com.getgame.scheduler.GetGamesScheduler
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

  private def getGamesScheduler[F[_]: Async: Logger](repo: GamesRepository[F]): F[Unit] = {
    val getGameScheduler = new GetGamesScheduler[F](1.hours)
    getGameScheduler.scheduleTask(repo)
  }

  def make[F[_]: Async: Console: Logger]: Resource[F, Environment[F]] =
    for {
      config <- Resource.eval(ConfigLoader.load[F, Config])
      _ <- Resource.eval(Migrations.run[F](config.migrations))
      implicit0(dispatcher: Dispatcher[F]) <- Dispatcher.parallel[F]
      implicit0(context: DBContext) = new DoobieContext.Postgres(myNamingStrategy)
      implicit0(xa: Transactor[F]) = makeTransactor(config.database)
      repos = Repositories.make[F]
      graphql = graphQL[F](repos)
      _ <- Resource.eval(getGamesScheduler[F](repos.games).start)
      _ = Logger[F].info(s"RUNNING AS ASYNC")
    } yield Environment[F](config, graphql)
}
