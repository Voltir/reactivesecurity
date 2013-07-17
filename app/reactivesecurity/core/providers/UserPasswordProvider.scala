package reactivesecurity.core.providers

import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.std.{AuthenticationServiceFailure, AuthenticationFailure}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{RequiresUsers, UsingID, CredentialValidator}
import scala.concurrent.{ExecutionContext,  Future}
import ExecutionContext.Implicits.global

case class IdPass[ID](id: ID, password: String)

trait UserPasswordProvider[ID,USER <: UsingID[ID]] extends AuthenticationService[IdPass[ID],USER,AuthenticationFailure] with RequiresUsers[ID,USER] {

  val credentialsValidator: CredentialValidator[USER, IdPass[ID], AuthenticationFailure]

  override def authenticate(credentials: IdPass[ID]): Future[Validation[AuthenticationFailure,USER]] =  {
    println("TODO -- authenticate -- UserPassProvider: "+credentials)
    users.find(credentials.id).map { check =>
      check match {
        case Failure(f) => Failure(AuthenticationServiceFailure(f))
        case Success(user) => credentialsValidator.validate(user,credentials)
      }
    }
  }
}