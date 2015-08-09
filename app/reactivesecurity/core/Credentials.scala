package reactivesecurity.core

import reactivesecurity.core.service.HasID

import scalaz.{Failure, Validation, Success}
import reactivesecurity.core.Password._
import reactivesecurity.core.providers.EmailPass
import reactivesecurity.core.Failures._
import scala.concurrent.{ExecutionContext, Future}

object Credentials {

  trait CredentialValidator[User <: HasID, CRED] {
    def validate(user: User, credential: CRED)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,User]]
  }

  trait PasswordHashValidator2[User <: HasID] extends CredentialValidator[User,EmailPass] {
    def passwordService: PasswordService[User]

    override def validate(user: User, credential: EmailPass)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure, User]] = {
      passwordService.find(user.id).map {
        case Some(pass) if passwordService.hasher.matches(pass, credential.password) => Success(user)
        case Some(_) => Failure(InvalidPassword)
        case None => Failure(CredentialsNotFound)
      }
    }
  }
}