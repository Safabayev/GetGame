package com.getgame.domain.freeToGameAPI
import io.circe.generic.JsonCodec

@JsonCodec
case class GameResp(
    id: Int,
    title: String,
    genre: String,
    platform: String,
    developer: String)
