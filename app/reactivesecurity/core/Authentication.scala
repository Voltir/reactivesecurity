package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future, future}

import reactivesecurity.core.User.{UsingID, UserProvider}

object Authentication {

  trait AuthenticationProcess[IN,OUT,USR] {
    def authentication(action: (IN,USR) => OUT): IN => OUT
  }

  trait AsyncAuthenticationProcess[IN,OUT,USR] {
    def authentication(action: (IN,USR) => OUT)(implicit ec: ExecutionContext): IN => Future[OUT]
  }

  trait InputValidator[IN,USER,FAILURE] {
    def validateInput(in: IN): Validation[FAILURE,USER]
  }

  trait AsyncInputValidator[IN,USER <: UsingID[_],FAILURE] {
    def validateInput(in: IN)(implicit ec: ExecutionContext): Future[Validation[FAILURE,USER]]
  }

  trait AuthenticationFailureHandler[IN,FAIL,OUT] {
    def onAuthenticationFailure(in: IN, failure: FAIL): OUT
  }

  trait NoAuthentication[IN,OUT,USR] extends AuthenticationProcess[IN,OUT,USR] {
    val userProvider: UserProvider[USR]
    override def authentication(action: (IN,USR) => OUT): IN => OUT = {
      in: IN => action(in,userProvider.user)
    }
  }

  trait Authentication[IN,OUT,USR,FAIL] extends AuthenticationProcess[IN,OUT,USR] {
    val inputValidator: InputValidator[IN,USR,FAIL]
    val authFailureHandler: AuthenticationFailureHandler[IN,FAIL,OUT]

    override def authentication(action: (IN,USR) => OUT): IN => OUT = {
      in: IN => {
        inputValidator.validateInput(in).fold(
          fail = { f => authFailureHandler.onAuthenticationFailure(in,f) },
          succ = { user => action(in,user) }
        )
      }
    }
  }

  trait AsyncAuthentication[IN,OUT,USER <: UsingID[_],FAILURE] extends AsyncAuthenticationProcess[IN,OUT,USER] {
    val inputValidator: AsyncInputValidator[IN,USER,FAILURE]
    val authFailureHandler: AuthenticationFailureHandler[IN,FAILURE,OUT]

    override def authentication(action: (IN,USER) => OUT)(implicit ec: ExecutionContext): IN => Future[OUT] = {
      in: IN => {
        inputValidator.validateInput(in).map { result =>
          result.fold(
            fail = { f => authFailureHandler.onAuthenticationFailure(in,f)  },
            succ = { user => action(in,user) }
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
