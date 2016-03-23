package reactivesecurity.controllers

import reactivesecurity.core.Authentication.{AuthenticationFailureHandler, AuthenticationProcess}
import scala.concurrent.{ExecutionContext, Future}

trait PlayAuthentication[IN,OUT,User,FAILURE] extends AuthenticationProcess[IN,OUT,User] {

  def authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

  //override def ec: ExecutionContext

  def validate(in: IN): Future[Either[FAILURE,User]]

  override def authentication[A <: IN](block: User => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
    in: A => {
      validate(in).flatMap { validation =>
        validation.fold(
          f => authFailureHandler.onAuthenticationFailure(in)(f),
          user => block(user)
        )
      }(ec)
    }
  }
}
