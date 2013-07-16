package reactivesecurity.core

import play.api.mvc._

import reactivesecurity.core.Authentication.{AsyncAuthentication, Authentication}
import reactivesecurity.core.std.AuthenticationFailure
import scala.concurrent.ExecutionContext
import reactivesecurity.core.User.UsingID

trait Secured[A,USER <: UsingID[_]] extends Controller with Authentication[Request[A],Result,USER,AuthenticationFailure] {

  def SecuredAction(p: BodyParser[A])(f: (Request[A],USER) => Result) = Action(p) { implicit request =>
    authentication(f)(request)
  }
}
trait AsyncSecured[A,USER <: UsingID[_]] extends Controller with AsyncAuthentication[Request[A],Result,USER,AuthenticationFailure] {
  def AsyncSecuredAction(p: BodyParser[A])(f: (Request[A],USER) => Result)(implicit ec: ExecutionContext) = Action(p) { implicit request =>
    Async {
      authentication(f)(ec)(request)
    }
  }
}
