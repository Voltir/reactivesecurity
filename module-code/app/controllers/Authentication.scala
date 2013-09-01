package reactivesecurity.controllers

import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.Authentication.AsyncAuthentication
import reactivesecurity.core.std.AuthenticationFailure
import reactivesecurity.core.User.UsingID

trait Authentication[USER <: UsingID] extends Controller with AsyncAuthentication[RequestHeader,Result,USER,AuthenticationFailure] {
  
  def Authenticated[A](p: BodyParser[A])(f: Request[A] => USER => Future[Result])(implicit ec: ExecutionContext): Action[A] = 
    Action(p) { implicit request: Request[A] => Async {
      authentication(f)(ec)(request)
  }}

  def Authenticated(f: Request[AnyContent] => USER => Future[Result])(implicit ec: ExecutionContext): Action[AnyContent] =
    Authenticated(parse.anyContent)(f)(ec)
}

