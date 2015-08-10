package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}

object Authentication {

  trait AuthenticationProcess[IN,OUT,+USER] {
    def ec: ExecutionContext
    def authentication[A <: IN](block: USER => Future[OUT]): A => Future[OUT]
  }

  trait AuthenticationFailureHandler[-IN,-FAIL,+OUT] {
    def onAuthenticationFailure(in: IN)(failure: FAIL): Future[OUT]
  }

  trait AuthenticationValidator[-CREDENTIALS, USER, +FAILURE] {
    def authenticate(credentials: CREDENTIALS)(implicit ctx: ExecutionContext): Future[Validation[FAILURE,USER]]
  }
}
