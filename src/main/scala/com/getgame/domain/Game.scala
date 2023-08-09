package com.getgame.domain

import java.time.ZonedDateTime
import java.util.UUID

import io.circe.generic.JsonCodec

@JsonCodec
case class Game(
    id: UUID,
    title: String,
    genre: String,
    platform: String,
    developer: String,
    createdAt: ZonedDateTime)
