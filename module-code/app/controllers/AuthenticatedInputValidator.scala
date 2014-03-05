package reactivesecurity.controllers

import reactivesecurity.core.Authentication.InputValidator
import reactivesecurity.core.Authenticator //merge??
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.Failures._

import concurrent.{ExecutionContext, Future}
import scalaz.{Success, Failure, Validation}
import play.api.mvc.RequestHeader

abstract class AuthenticatedInputValidator[USER <: UsingID] extends InputValidator[RequestHeader,USER,AuthenticationFailure] {
  val users: UserService[USER]
  val authenticator: Authenticator[USER]

  override def validateInput(in: RequestHeader)(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure,USER]] = {
    val fail: Validation[UserServiceFailure,USER] = Failure(ValidationFailure)
    authenticator.find(in).flatMap {
      case Some(token) => users.find(token.uid).map(_.fold(fail)(Success(_)))
      case _ => Future(fail)
    }
  }
}
