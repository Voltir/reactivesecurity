package core

import core.User.UsingID
import scala.concurrent.{Future, ExecutionContext}

object Password {
  case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

  trait PasswordHasher {
    def id: String

    def hash(plainPassword: String): PasswordInfo

    def matches(passwordInfo: PasswordInfo, supplied: String): Boolean
  }

  trait PasswordService[USER <: UsingID] {
    val hasher: PasswordHasher

    def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[PasswordInfo]]

    def save(id: USER#ID, pass: PasswordInfo)(implicit ec: ExecutionContext): Future[Boolean]
  }
}