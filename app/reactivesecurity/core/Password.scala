package reactivesecurity.core

import com.softwaremill.macwire.MacwireMacros._

import reactivesecurity.core.service.HasID

import scala.concurrent.{Future, ExecutionContext}

object Password {
  case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

  trait PasswordHasher {
    def id: String
    def hash(plainPassword: String): PasswordInfo
    def matches(passwordInfo: PasswordInfo, supplied: String): Boolean
  }

  trait PasswordService[User <: HasID] {
    def hasher: PasswordHasher
    def find(id: User#ID)(implicit ec: ExecutionContext): Future[Option[PasswordInfo]]
  }
}