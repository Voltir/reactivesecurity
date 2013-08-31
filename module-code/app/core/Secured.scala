package reactivesecurity.core

import play.api.mvc._

import reactivesecurity.core.Authentication.AsyncAuthentication
import reactivesecurity.core.std.AuthenticationFailure
import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.User.UsingID

trait AsyncSecured[USER <: UsingID] extends Controller with AsyncAuthentication[RequestHeader,Result,USER,AuthenticationFailure] {
  
  def Secured[B](p: BodyParser[B])(f: Request[B] => USER => Result)(implicit ec: ExecutionContext): Action[B] = Action(p) { implicit request: Request[B] =>
    Async {
      authentication(f)(ec)(request)
    }
  }

  def Secured(f: Request[AnyContent] => USER => Result)(implicit ec: ExecutionContext): Action[AnyContent] =
    Secured(parse.anyContent)(f)(ec)
}

