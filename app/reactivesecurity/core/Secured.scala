package reactivesecurity.core

import play.api.mvc._

import reactivesecurity.core.Authentication.{Authentication}
import reactivesecurity.core.std.AuthenticationFailure
import scala.concurrent.ExecutionContext

trait Secured[A,USER] extends Controller with Authentication[Request[A],Result,USER,AuthenticationFailure] {

  def SecuredAction(p: BodyParser[A])(f: (Request[A],USER) => Result) = Action(p) { implicit request =>
    authentication(f)(request)
  }

  def AsyncSecuredAction(p: BodyParser[A])(f: (Request[A],USER) => Result)(implicit ec: ExecutionContext) = Action(p) { implicit request =>
    Async {
      asyncAuthentication(f)(ec)(request)
    }
  }
}
