package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future, future}

import reactivesecurity.core.User.UsingID

object Authentication {

  trait AsyncAuthenticationProcess[IN,OUT,USER] {
    def futureAuthentication[A <: IN](action: A => USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT]
    def authentication[A <: IN](action: A => USER => OUT)(implicit ec: ExecutionContext): A => Future[OUT]
  }

  trait AsyncInputValidator[-IN,USER <: UsingID,FAILURE] {
    def validateInput(in: IN)(implicit ec: ExecutionContext): Future[Validation[FAILURE,USER]]
  }

  trait AuthenticationFailureHandler[IN,FAIL,OUT] {
    def onAuthenticationFailure(in: IN, failure: FAIL): OUT
  }

  trait AsyncAuthentication[IN,OUT,USER <: UsingID,FAILURE] extends AsyncAuthenticationProcess[IN,OUT,USER] {
    val inputValidator: AsyncInputValidator[IN,USER,FAILURE]
    val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

    override def futureAuthentication[A <: IN](action: A => USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[OUT] = {
      in: A => {
        inputValidator.validateInput(in).flatMap { result =>
          result.fold(
            fail = { f => future { authFailureHandler.onAuthenticationFailure(in,f) }  },
            succ = { user => action(in)(user) }
          )
        }
      }
    }

    override def authentication[A <: IN](action: A => USER => OUT)(implicit ec: ExecutionContext): A => Future[OUT] = {
      in: A => {
        inputValidator.validateInput(in).map { result =>
          result.fold(
            fail = { f => authFailureHandler.onAuthenticationFailure(in,f)  },
            succ = { user => action(in)(user) }
          )
        }
      }
    }
  }

  trait AuthenticationService[-CREDENTIALS, USER, +FAILURE] {
    def authenticate(credentials: CREDENTIALS): Future[Validation[FAILURE,USER]]
  }

  trait AuthorizationService[-USER,-RESOURCE,+FAILURE] {
    def authorize(user: USER, resource: RESOURCE): Future[Boolean]
  }
}
