package reactivesecurity.core

import reactivesecurity.core.User.{UserService, UsingID}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.Authentication.InputValidator
import concurrent.{ExecutionContext, Future}
import play.api.mvc.RequestHeader

object std {

  trait AuthenticationFailure

  abstract class UserServiceFailure extends AuthenticationFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

  case class IdentityNotFound[USER <: UsingID](userid: USER#ID) extends UserServiceFailure

  case class ValidationFailure() extends UserServiceFailure

  case class InvalidPassword() extends AuthenticationFailure

  case class CredentialsNotFound() extends AuthenticationFailure

  case class OauthFailure(reason: String) extends AuthenticationFailure

  case class OauthNoVerifier() extends AuthenticationFailure

  case class OAuth2NoAccessCode() extends AuthenticationFailure

  //Move this to "play" dir
  abstract class AuthenticatedInputValidator[USER <: UsingID] extends InputValidator[RequestHeader,USER,AuthenticationFailure] {
    val users: UserService[USER]
    val authenticator: Authenticator

    override def validateInput(in: RequestHeader)(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure,USER]] = {
      val fail: Validation[UserServiceFailure,USER] = Failure(ValidationFailure())
      authenticator.find(in).fold(Future(fail)) {
        token => users.find(users.strAsId(token.uid)).map(_.fold(fail)(Success(_)))
      }
    }
  }
}
