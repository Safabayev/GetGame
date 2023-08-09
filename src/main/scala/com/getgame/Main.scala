package com.getgame

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits.toTraverseOps
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.getgame.server.HttpServer

object Main extends IOApp {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def runnable: Resource[IO, List[IO[ExitCode]]] =
    for {
      env <- Environment.make[IO]
      httpServer <- HttpServer.run[IO](env)
    } yield List(httpServer)

  override def run(
      args: List[String]
    ): IO[ExitCode] =
    runnable.use { runners =>
      for {
        fibers <- runners.traverse(_.start)
        _ <- fibers.traverse(_.join)
        _ <- IO.never[ExitCode]
      } yield ExitCode.Success
    }
}
