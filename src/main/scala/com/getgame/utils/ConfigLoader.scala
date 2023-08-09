package com.getgame.utils

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeErrorId
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object ConfigLoader {
  def load[F[_]: Sync, Conf: ConfigReader: ClassTag]: F[Conf] =
    EitherT
      .fromEither[F](
        ConfigSource
          .file("conf/local.conf")
          .recoverWith(_ => ConfigSource.resources("local.conf"))
          .recoverWith(_ => ConfigSource.default)
          .load[Conf]
      )
      .valueOrF { failures =>
        new RuntimeException(s"Boom 💥💥💥 \n${failures.prettyPrint(2)}").raiseError[F, Conf]
      }
}