package reactivesecurity.controllers

import reactivesecurity.core.{AuthenticationToken, AuthenticatorService, Authenticator}

//merge??
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.Failures._

import concurrent.{ExecutionContext, Future}
import scalaz.{Success, Failure, Validation}
import play.api.mvc.RequestHeader

abstract class AuthenticatedInputValidator[User <: UsingID] extends InputValidator[RequestHeader,User,AuthenticationFailure] {
  //val users: UserService[USER]
  //val authenticator: Authenticator[USER]
  val authService: AuthenticatorService[User,AuthenticationFailure]
  def extractAuthenticationToken(req: RequestHeader): Option[AuthenticationToken]

  override def apply(in: RequestHeader)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,User]] = {
    extractAuthenticationToken(in).map { token =>
      authService.get(token)
    }.getOrElse {
      Future(Failure(AuthenticationTokenNotFound))
    }
  }
}
