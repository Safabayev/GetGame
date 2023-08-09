package com.getgame.api

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import sangria.ast
import sangria.macros.derive.{deriveInputObjectType, deriveObjectType}
import sangria.schema.{InputObjectType, ListType, ObjectType, ScalarType}
import sangria.validation.ValueCoercionViolation
import com.getgame.domain.Game
import com.getgame.repos.Repositories

package object graphql {
  type Ctx[F[_]] = Repositories[F]
  private case object UUIDCoercionViolation extends ValueCoercionViolation("Invalid UUID format")

  private case object DateTimeCoercionViolation
      extends ValueCoercionViolation(
        "Invalid ZonedDateTime value"
      )

  implicit val UuidType: ScalarType[UUID] = ScalarType[UUID](
    "UUID",
    coerceOutput = (value, _) => value.toString,
    coerceUserInput = {
      case s: String =>
        try Right(UUID.fromString(s))
        catch {
          case _: IllegalArgumentException => Left(UUIDCoercionViolation)
        }
      case _ => Left(UUIDCoercionViolation)
    },
    coerceInput = {
      case s: ast.StringValue =>
        try Right(UUID.fromString(s.value))
        catch {
          case _: IllegalArgumentException => Left(UUIDCoercionViolation)
        }
      case _ => Left(UUIDCoercionViolation)
    }
  )
  implicit val ZonedDateTimeType: ScalarType[ZonedDateTime] = ScalarType[ZonedDateTime](
    "ZonedDateTime",
    coerceOutput = (date, _) => DateTimeFormatter.ISO_INSTANT.format(date),
    coerceInput = (input: Any) =>
      input match {
        case s: String =>
          try Right(ZonedDateTime.parse(s))
          catch {
            case _: DateTimeParseException => Left(DateTimeCoercionViolation)
          }
        case _ => Left(DateTimeCoercionViolation)
      },
    coerceUserInput = (input: Any) =>
      input match {
        case s: String =>
          try Right(ZonedDateTime.parse(s))
          catch {
            case _: DateTimeParseException => Left(DateTimeCoercionViolation)
          }
        case _ => Left(DateTimeCoercionViolation)
      }
  )

  implicit val GameType: ObjectType[Unit, Game] = deriveObjectType[Unit, Game]()
  implicit val GameInputType: InputObjectType[Game] = deriveInputObjectType[Game]()
  implicit val GameListType: ListType[Game] = ListType(GameType)
}
