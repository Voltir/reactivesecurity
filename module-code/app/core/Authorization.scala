package core

import scala.concurrent.Future

object Authorization {

  //trait AuthorizationService[-USER,-RESOURCE] {
  //  def authorize(user: USER, resource: RESOURCE): Future[Boolean]
  //}

  trait AuthorizationService[-USER] {
    def authorize(user: USER): Future[Boolean]
  }
}
