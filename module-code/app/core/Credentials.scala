package core

import scalaz.{Failure, Validation, Success}

import reactivesecurity.core.User.UsingID
import reactivesecurity.core.Password._
import reactivesecurity.core.providers.EmailPass
import reactivesecurity.core.Failures._
import scala.concurrent.{ExecutionContext, Future}

object Credentials {

  trait CredentialValidator[USER <: UsingID, CRED] {
    def validate(user: USER, credential: CRED)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]]
  }

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