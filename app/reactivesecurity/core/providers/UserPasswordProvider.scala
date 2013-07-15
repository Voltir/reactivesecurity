package reactivesecurity.core.providers

import play.api.data.Form
import play.api.data.Forms._
import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.std.{AuthenticationServiceFailure, AuthenticationFailure}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{UserService, CredentialValidator}
import scala.concurrent.{ExecutionContext,  Future}
import ExecutionContext.Implicits.global

case class IdPass[ID](id: ID, password: String)

trait UserPasswordProvider[ID,USER] extends AuthenticationService[IdPass[ID],USER,AuthenticationFailure] {

  val userService: UserService[ID,AuthenticationFailure, USER]
  val credentialsValidator: CredentialValidator[USER, IdPass[ID], AuthenticationFailure]

  def authenticate(credentials: IdPass[ID]): Future[Validation[AuthenticationFailure,USER]] =  {
    userService.findById(credentials.id).map { check =>
      check match {
        case Failure(f) => Failure(AuthenticationServiceFailure(f))
        case Success(user) => credentialsValidator.validate(user,credentials)
      }
    }
  }
}

object UserPasswordProvider {
  val loginForm = Form[(String,String)](
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )
}
