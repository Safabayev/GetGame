package com.getgame.integration
import com.getgame.domain.freeToGameAPI.GameResp
import sttp.client3.circe.asJson
import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend, UriContext, basicRequest}
class FreeGameApiClient {
  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  def getGames: List[GameResp] = {
    val apiUrl = uri"https://www.freetogame.com/api/games"
    val request = basicRequest.get(apiUrl).response(asJson[List[GameResp]])

    val response = request.send(backend)

    response.body match {
      case Right(games) => games
      case Left(error) => throw new RuntimeException(s"Error getting games: ${error.getMessage}")
    }
  }

}