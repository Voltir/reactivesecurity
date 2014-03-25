package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}

import reactivesecurity.core.User.UsingID

sealed trait ValidationResult
case class ValidationFailure[FAILURE](f: FAILURE)
case class Valid[USER <: UsingID](user: USER)
case class SetCookie[USER <: UsingID](user: USER)

object Authentication {

  trait AuthenticationProcess[IN,OUT,USER] {
    def authentication[A <: IN](block: USER => Future[OUT])(implicit ec: ExecutionContext): A => Future[(Int,OUT)]
  }

  trait AuthenticationFailureHandler[IN,FAIL,OUT] {
    def onAuthenticationFailure(in: IN, failure: FAIL): Future[OUT]
  }

  trait AuthenticationService[-CREDENTIALS, USER, +FAILURE] {
    def authenticate(credentials: CREDENTIALS): Future[Validation[FAILURE,USER]]
  }
}
