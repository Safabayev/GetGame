package com.getgame.api.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._

import com.getgame.api.graphql.GraphQL

final case class GraphQLRoutes[F[_]: JsonDecoder: MonadThrow](
    graphQL: GraphQL[F])
    extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "graphql" =>
      req.asJson.flatMap(graphQL.query).flatMap {
        case Right(json) => Ok(json)
        case Left(json) => BadRequest(json)
      }
  }
}
