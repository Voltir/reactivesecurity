package reactivesecurity.core

import reactivesecurity.core.User.{AsID, UserService, UsingID}
import scalaz.{Failure,Validation}
import reactivesecurity.core.Authentication.AsyncInputValidator
import scala.concurrent.{ExecutionContext, future, Future}
import play.api.mvc.{AnyContent, Request, RequestHeader}
import play.api.data.Form
import play.api.data.Forms._
import scalaz.Failure

object std {
  trait AuthenticationFailure

  abstract class UserServiceFailure extends AuthenticationFailure

  case class IdentityNotFound[USER <: UsingID](userid: USER#ID) extends UserServiceFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

  case class ValidationFailure() extends UserServiceFailure

  case class InvalidPassword() extends AuthenticationFailure


  abstract class AuthenticatedInputValidator[USER <: UsingID] extends AsyncInputValidator[Request[AnyContent],USER,AuthenticationFailure] {
    val users: UserService[USER]
    val authenticator: reactivesecurity.core.Authenticator
    val asID: AsID[USER]

    override def validateInput(in: Request[AnyContent])(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure,USER]] = {
      authenticator.find(in).map(
        token => users.find(asID(token.uid))
      ).getOrElse(future { Failure[UserServiceFailure,USER](ValidationFailure().asInstanceOf[UserServiceFailure]) } )
    }
  }
}
