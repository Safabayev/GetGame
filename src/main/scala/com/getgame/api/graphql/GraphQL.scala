package com.getgame.api.graphql

import io.circe.Json
import io.circe.JsonObject

trait GraphQL[F[_]] {
  def query(request: Json): F[Either[Json, Json]]
  def query(
      query: String,
      operationName: Option[String],
      variables: JsonObject
    ): F[Either[Json, Json]]
}
