package core

import scalaz.{Failure, Validation, Success}

import reactivesecurity.core.User.UsingID
import reactivesecurity.core.Password._
import reactivesecurity.core.providers.IdPass
import reactivesecurity.core.std.{CredentialsNotFound, InvalidPassword, AuthenticationFailure}
import scala.concurrent.{ExecutionContext, Future}

object Credentials {

  trait CredentialValidator[USER <: UsingID, CRED] {
    def validate(user: USER, credential: CRED)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]]
  }

  class AlwaysValid[USER <: UsingID] extends CredentialValidator[USER,IdPass] {
    override def validate(user: USER, credential: IdPass)(implicit ec: ExecutionContext) = Future(Success(user))
  }

  trait PasswordHashValidator[USER <: UsingID] extends CredentialValidator[USER,IdPass] {
    val passwordService: PasswordService[USER]

    override def validate(user: USER, credential: IdPass)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]] = {
      passwordService.find(user.id).map { _.map { storedPass =>
        if(passwordService.hasher.matches(storedPass,credential.password)) Success(user)
        else Failure(InvalidPassword())
      }.getOrElse(Failure(CredentialsNotFound()))
    }}
  }
}