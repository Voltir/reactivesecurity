package reactivesecurity.core

import reactivesecurity.core.User.{UserService, UsingID}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.Authentication.AsyncInputValidator
import scala.concurrent.{ExecutionContext, future, Future}
import play.api.mvc.{AnyContent, RequestHeader}

object std {

  trait AuthenticationFailure

  abstract class UserServiceFailure extends AuthenticationFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

  case class IdentityNotFound[USER <: UsingID](userid: USER#ID) extends UserServiceFailure

  case class ValidationFailure() extends UserServiceFailure

  case class InvalidPassword() extends AuthenticationFailure

  case class CredentialsNotFound() extends AuthenticationFailure

  //Move this to "play" dir
  abstract class AuthenticatedInputValidator[USER <: UsingID] extends AsyncInputValidator[RequestHeader,USER,AuthenticationFailure] {
    val users: UserService[USER]
    val authenticator: Authenticator

    override def validateInput(in: RequestHeader)(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure,USER]] = {
      val fail: Validation[UserServiceFailure,USER] = Failure(ValidationFailure())
      authenticator.find(in).fold(future { fail }) {
        token => users.find(users.idFromEmail(token.uid)).map(_.fold(fail)(Success(_)))
      }
    }
  }
}
