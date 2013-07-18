package reactivesecurity.core.providers

import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.std.{AuthenticationServiceFailure, AuthenticationFailure}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{RequiresUsers, UsingID, CredentialValidator}
import scala.concurrent.{ExecutionContext,  Future, future}
import ExecutionContext.Implicits.global

case class IdPass[USER <: UsingID](id: USER#ID, password: String)

trait UserPasswordProvider[USER <: UsingID] extends AuthenticationService[IdPass[USER],USER,AuthenticationFailure] with RequiresUsers[USER] {

  val credentialsValidator: CredentialValidator[USER, IdPass[USER]]

  override def authenticate(credentials: IdPass[USER]): Future[Validation[AuthenticationFailure,USER]] =  future {

    users.find(credentials.id).map { check =>
      check match {
        case Failure(f) => Failure(AuthenticationServiceFailure(f))
        case Success(user) => credentialsValidator.validate(user,credentials)
      }
    }
  }
}