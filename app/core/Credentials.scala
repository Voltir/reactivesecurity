package core

import scalaz.{Failure, Validation, Success}

import core.User.UsingID
import core.Password._
import core.providers.EmailPass
import core.Failures._
import scala.concurrent.{ExecutionContext, Future}

object Credentials {

  trait CredentialValidator[USER <: UsingID, CRED] {
    def validate(user: USER, credential: CRED)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]]
  }

  /* TODO DELETE
  class AlwaysValid[USER <: UsingID] extends CredentialValidator[USER,IdPass] {
    override def validate(user: USER, credential: IdPass)(implicit ec: ExecutionContext) = Future(Success(user))
  }

  trait PasswordHashValidator[USER <: UsingID] extends CredentialValidator[USER,IdPass] {
    val passwordService: PasswordService[USER]

    override def validate(user: USER, credential: IdPass)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]] = {
      passwordService.find(user.id).map { _.map { storedPass =>
        if(passwordService.hasher.matches(storedPass,credential.password)) Success(user)
        else Failure(InvalidPassword)
      }.getOrElse(Failure(CredentialsNotFound))
    }}
  }
  */
  trait PasswordHashValidator2[USER <: UsingID] extends CredentialValidator[USER,EmailPass] {
    val passwordService: PasswordService[USER]

    override def validate(user: USER, credential: EmailPass)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]] = {
      passwordService.find(user.id).map { _.map { storedPass =>
        if(passwordService.hasher.matches(storedPass,credential.password)) Success(user)
        else Failure(InvalidPassword)
      }.getOrElse(Failure(CredentialsNotFound))
      }}
  }
}