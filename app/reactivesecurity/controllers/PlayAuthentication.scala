package reactivesecurity.controllers

import reactivesecurity.core.Authentication.{AuthenticationFailureHandler, AuthenticationProcess}
import reactivesecurity.core.service.HasID
import scala.concurrent.{ExecutionContext, Future}

trait PlayAuthentication[IN,OUT,User <: HasID,FAILURE] extends AuthenticationProcess[IN,OUT,User] {
  val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

  def validate(in: IN)(implicit ec: ExecutionContext): Future[Either[FAILURE,User]]

  override def authentication[A <: IN](block: User => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
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
