package reactivesecurity.core

import reactivesecurity.core.User.{IdFromString, UsingID}
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

  case class IdentityNotFound[ID](username: ID) extends UserServiceFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

  case class ValidationFailure() extends UserServiceFailure

  //Standard IdFromString
  object StringFromString extends IdFromString[String] {
    override def apply(id: String) = id
  }

  abstract class AuthenticatedInputValidator[ID,USER <: UsingID[ID]] extends AsyncInputValidator[Request[AnyContent],USER,AuthenticationFailure] {

    val authenticator: reactivesecurity.core.Authenticator
    val users: reactivesecurity.core.User.UserService[ID,USER,UserServiceFailure]
    val str2id: reactivesecurity.core.User.IdFromString[ID]

    override def validateInput(in: Request[AnyContent])(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure,USER]] = {
      authenticator.find(in).map(
        token => users.find(str2id(token.uid))
      ).getOrElse(future { Failure[UserServiceFailure,USER](ValidationFailure().asInstanceOf[UserServiceFailure]) } )
    }
  }
}
