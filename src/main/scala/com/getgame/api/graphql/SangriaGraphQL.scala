package com.getgame.api.graphql

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import cats.effect._
import cats.implicits._
import io.circe.Json
import io.circe.JsonObject
import sangria.ast._
import sangria.execution.WithViolations
import sangria.execution._
import sangria.execution.deferred._
import sangria.marshalling.circe
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.parser.SyntaxError
import sangria.schema._
import sangria.validation._

object SangriaGraphQL {
  private def formatSyntaxError(e: SyntaxError): Json = Json.obj(
    "errors" -> Json.arr(
      Json.obj(
        "message" -> Json.fromString(e.getMessage),
        "locations" -> Json.arr(
          Json.obj(
            "line" -> Json.fromInt(e.originalError.position.line),
            "column" -> Json.fromInt(e.originalError.position.column)
          )
        )
      )
    )
  )

  private def formatWithViolations(e: WithViolations): Json =
    Json.obj("errors" -> Json.fromValues(e.violations.map {
      case v: AstNodeViolation =>
        Json.obj(
          "message" -> Json.fromString(v.errorMessage),
          "locations" -> Json.fromValues(
            v.locations
              .map(loc => Json.obj("line" -> Json.fromInt(loc.line), "column" -> Json.fromInt(loc.column)))
          )
        )
      case v => Json.obj("message" -> Json.fromString(v.errorMessage))
    }))

  private def formatString(s: String): Json =
    Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(s))))

  // Format a Throwable as a GraphQL `errors`
  private def formatThrowable(e: Throwable): Json = Json.obj(
    "errors" -> Json.arr(
      Json.obj(
        "class" -> Json.fromString(e.getClass.getName),
        "message" -> Json.fromString(e.getMessage)
      )
    )
  )
  def apply[F[_]] = new Partial[F]
  final class Partial[F[_]] {
    def apply[A](
        schema: Schema[A, Unit],
        deferredResolver: DeferredResolver[A],
        userContext: F[A],
        blockingExecutionContext: ExecutionContext
      )(implicit
        F: Async[F]
      ): GraphQL[F] =
      new GraphQL[F] {

        // Destructure `request` and delegate to the other overload.
        def query(request: Json): F[Either[Json, Json]] = {
          val queryString = request.hcursor.downField("query").as[String].toOption
          val operationName = request.hcursor.downField("operationName").as[String].toOption
          val variables =
            request.hcursor.downField("variables").as[JsonObject].getOrElse(JsonObject.empty)

          queryString match {
            case Some(qs) => query(qs, operationName, variables)
            case None => fail(formatString("No 'query' property was present in the request."))
          }
        }

        // Parse `query` and execute.
        def query(
            query: String,
            operationName: Option[String],
            variables: JsonObject
          ): F[Either[Json, Json]] =
          QueryParser.parse(query) match {
            case Success(ast) =>
              exec(schema, userContext, ast, operationName, variables)(blockingExecutionContext)
            case Failure(e @ SyntaxError(_, _, _)) => fail(formatSyntaxError(e))
            case Failure(e) => fail(formatThrowable(e))
          }

        // Lift a `Json` into the error side of our effect.
        def fail(j: Json): F[Either[Json, Json]] =
          F.pure(j.asLeft)

        // Execute a GraphQL query with Sangria, lifting into IO for safety and sanity.
        def exec(
            schema: Schema[A, Unit],
            userContext: F[A],
            query: Document,
            operationName: Option[String],
            variables: JsonObject
          )(implicit
            ec: ExecutionContext
          ): F[Either[Json, Json]] =
          userContext
            .flatMap { ctx =>
              F.async_ { (cb: Either[Throwable, Json] => Unit) =>
                Executor
                  .execute(
                    schema = schema,
                    deferredResolver = deferredResolver,
                    queryAst = query,
                    userContext = ctx,
                    variables = Json.fromJsonObject(variables),
                    operationName = operationName,
                    exceptionHandler = ExceptionHandler {
                      case (_, e) => HandledException(e.getMessage)
                    }
                  )
                  .onComplete[Unit] {
                    case Success(value: circe.CirceResultMarshaller.Node) =>
                      cb(Right(value.dropNullValues))
                    case Failure(error) => cb(Left(error))
                  }
              }
            }
            .attempt
            .flatMap {
              case Right(json) => F.pure(json.asRight)
              case Left(err: WithViolations) => fail(formatWithViolations(err))
              case Left(err) => fail(formatThrowable(err))
            }
      }
  }
}
