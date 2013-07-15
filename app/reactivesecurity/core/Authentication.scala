package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future, future}

import reactivesecurity.core.User.UserProvider

object Authentication {

  trait AuthenticationProcess[IN,OUT,USR] {
    def authentication(action: (IN,USR) => OUT): IN => OUT
    def asyncAuthentication(action: (IN,USR) => OUT)(implicit ec: ExecutionContext): IN => Future[OUT]
  }

  trait AsyncAuthenticationProcess[IN,OUT,USR] {
    def authentication(action: (IN,USR))(implicit ec: ExecutionContext): IN => Future[OUT]
  }

  trait InputValidator[-IN,+USR,+FAIL] {
    def validateInput(in: IN): Validation[FAIL,USR]
  }

  trait AsyncInputValidator[-IN,+USR,+FAIL] {
    def validateInput(in: IN): Future[Validation[FAIL,USR]]
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

    override def asyncAuthentication(action: (IN,USR) => OUT)(implicit ec: ExecutionContext): IN => Future[OUT] = {
      in: IN => {
        future { inputValidator.validateInput(in) }.map { result =>
          result.fold(
            fail = { f => authFailureHandler.onAuthenticationFailure(in,f)  },
            succ = { user => action(in,user) }
          )
        }
      }
    }
  }

  trait AuthenticationService[-Credentials, User, +F] {
    def authenticate(credentials: Credentials): Future[Validation[F,User]]
  }

  trait AuthorizationService[-USER,-RESOURCE,+FAILURE] {
    def authorize(user: USER, resource: RESOURCE): Future[Boolean]
  }

  trait LoginHandler[-IN,OUT] {
    def getLoginPage(in: IN): OUT
  }
}
