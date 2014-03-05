package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}

import reactivesecurity.core.User.UsingID

object Authentication {

  trait AuthenticationProcess[IN,OUT,USER] {
    def authentication[A <: IN](block: USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT]
  }

  trait InputValidator[-IN,USER <: UsingID,FAILURE] {
    def validateInput(in: IN)(implicit ec: ExecutionContext): Future[Validation[FAILURE,USER]]
  }

  trait AuthenticationFailureHandler[IN,FAIL,OUT] {
    def onAuthenticationFailure(in: IN, failure: FAIL): Future[OUT]
  }

  trait AsyncAuthentication[IN,OUT,USER <: UsingID,FAILURE] extends AuthenticationProcess[IN,OUT,USER] {
    val inputValidator: InputValidator[IN,USER,FAILURE]
    val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

    override def authentication[A <: IN](block: USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
      in: A => {
        inputValidator.validateInput(in).flatMap { validation =>
          validation.fold(
            fail = { f => authFailureHandler.onAuthenticationFailure(in,f) },
            succ = { user =>
              block(user)
            }
          )
        }
      }
    }
  }

  trait AuthenticationService[-CREDENTIALS, USER, +FAILURE] {
    def authenticate(credentials: CREDENTIALS): Future[Validation[FAILURE,USER]]
  }
}
