package reactivesecurity.controllers

import reactivesecurity.core.Authentication.{AuthenticationFailureHandler, AuthenticationProcess}
import reactivesecurity.core.User.UsingID
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Validation

trait InputValidator[-IN,USER <: UsingID,FAILURE] {
  def validateInput(in: IN)(implicit ec: ExecutionContext): Future[Validation[FAILURE,USER]]
}

//todo -- contemplate the idea of a "Output modifier": AuthToken => Out => Out [ostensibly modified]
trait PlayAuthentication[IN,OUT,USER <: UsingID,FAILURE] extends AuthenticationProcess[IN,OUT,USER] {
  val inputValidator: InputValidator[IN,USER,FAILURE]
  val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

  override def authentication[A <: IN](block: USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
    in: A => {
      inputValidator.validateInput(in).flatMap { validation =>
        validation.fold(
          fail = { f => authFailureHandler.onAuthenticationFailure(in)(f) },
          succ = { user => block(user) }
        )
      }
    }
  }
}