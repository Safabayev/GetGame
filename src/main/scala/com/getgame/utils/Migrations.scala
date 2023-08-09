package com.getgame.utils

import java.sql.DriverManager

import cats.effect.Sync
import cats.implicits.toFlatMapOps
import cats.syntax.all._
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.Logger

object Migrations {
  case class MigrationsConfig(
      hostname: String,
      port: Int,
      database: String,
      username: String,
      password: String,
      schema: String,
      location: String,
    ) {
    lazy val rootUrl: String =
      s"jdbc:postgresql://$hostname:$port/$database?user=$username&password=$password"

    lazy val url: String =
      s"jdbc:postgresql://$hostname:$port/$database"
  }

  def run[F[_]: Sync](
      config: MigrationsConfig
    )(implicit
      logger: Logger[F]
    ): F[Unit] =
    for {
      _ <- Sync[F].unit
      conn = DriverManager.getConnection(config.rootUrl)
      stmt = conn.createStatement()
      _ = stmt.execute(s"CREATE SCHEMA IF NOT EXISTS ${config.schema}")
      _ = stmt.closeOnCompletion()
      _ <- logger.info(s"Created schema if it didnt exist: ${config.schema}")
      flyway = Flyway
        .configure()
        .dataSource(config.url, config.username, config.password)
        .locations(config.location)
        .schemas(config.schema)
        .baselineOnMigrate(true)
        .table("schema_history")
        .load()
      _ <- Sync[F]
        .blocking(flyway.migrate())
        .void
        .onError(err => logger.error(err)("Migration run error"))
    } yield {}
}
