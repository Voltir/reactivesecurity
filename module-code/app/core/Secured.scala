package reactivesecurity.core

import play.api.mvc._

import reactivesecurity.core.Authentication.AsyncAuthentication
import reactivesecurity.core.std.AuthenticationFailure
import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.User.UsingID

trait AsyncSecured[A,USER <: UsingID] extends Controller with AsyncAuthentication[Request[A],Result,USER,AuthenticationFailure] {
  def Secured(p: BodyParser[A])(f: Request[A] => USER => Result)(implicit ec: ExecutionContext): Action[A] = Action(p) { implicit request: Request[A] =>
    Async {
      authentication(f)(ec)(request)
    }
  }
}

trait AnyContentAsyncSecured[USER <: UsingID] extends Controller with AsyncSecured[AnyContent,USER] {
  def Secured(f: Request[AnyContent] => USER => Result)(implicit ec: ExecutionContext): Action[AnyContent] =
    Secured(parse.anyContent)(f)(ec)
}

