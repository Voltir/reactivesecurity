package reactivesecurity.core

import scala.concurrent.Future

object Authorization {
  trait AuthorizationService[-USER] {
    def authorize(user: USER): Future[Boolean]
  }
}
