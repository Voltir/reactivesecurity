package reactivesecurity.controllers

import reactivesecurity.core.Authentication.{AuthenticationFailureHandler, AuthenticationProcess}
import reactivesecurity.core.User.UsingID
import scala.concurrent.{ExecutionContext, Future}

trait PlayAuthentication[IN,OUT,USER <: UsingID,FAILURE] extends AuthenticationProcess[IN,OUT,USER] {
  val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

  def validate(in: IN)(implicit ec: ExecutionContext): Future[Either[FAILURE,USER]]

  override def authentication[A <: IN](block: USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
    in: A => {
      validate(in).flatMap { validation =>
        validation.fold(
          f => authFailureHandler.onAuthenticationFailure(in)(f),
          user => block(user)
        )
      }
    }
  }
}
