package controllers

import core.Authentication.{AuthenticationFailureHandler, AuthenticationProcess}
import core.AuthenticatorService
import core.User.UsingID
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Validation


trait InputValidator[-IN,USER <: UsingID,FAILURE] {
  def apply(in: IN)(implicit ec: ExecutionContext): Future[Validation[FAILURE,USER]]
}

trait PlayAuthentication[IN,OUT,USER <: UsingID,FAILURE] extends AuthenticationProcess[IN,OUT,USER] {
  val inputValidator: InputValidator[IN,USER,FAILURE]
  val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

  override def authentication[A <: IN](block: USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
    in: A => {
      inputValidator(in).flatMap { validation =>
        validation.fold(
          fail = { f => authFailureHandler.onAuthenticationFailure(in)(f) },
          succ = { user => block(user) }
        )
      }
    }
  }
}
