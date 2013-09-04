package reactivesecurity.controllers

import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.Authentication.AsyncAuthentication
import reactivesecurity.core.std.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc.WebSocket.FrameFormatter

trait Authentication[USER <: UsingID] extends Controller with AsyncAuthentication[RequestHeader,Result,USER,AuthenticationFailure] {

  def FutureAuthenticated[A](p: BodyParser[A])(f: Request[A] => USER => Future[Result])(implicit ec: ExecutionContext): Action[A] =
    Action(p) { implicit request: Request[A] => Async {
      futureAuthentication(f)(ec)(request)
    }}

  def FutureAuthenticated(f: Request[AnyContent] => USER => Future[Result])(implicit ec: ExecutionContext): Action[AnyContent] =
    FutureAuthenticated(parse.anyContent)(f)(ec)

  def Authenticated[A](p: BodyParser[A])(f: Request[A] => USER => Result)(implicit ec: ExecutionContext): Action[A] = Action(p) {
    implicit request: Request[A] => Async {
      authentication(f)(ec)(request)
    }
  }

  def Authenticated(f: Request[AnyContent] => USER => Result)(implicit ec: ExecutionContext): Action[AnyContent] =
    Authenticated(parse.anyContent)(f)(ec)

  def AuthenticatedWS[A](f: RequestHeader => USER => Future[(Iteratee[A, _], Enumerator[A])])(implicit frameFormatter: FrameFormatter[A], ec: ExecutionContext): WebSocket[A] = WebSocket.async[A] {
    request => inputValidator.validateInput(request).flatMap { result =>
      val foo: Iteratee[A,Nothing] = play.api.libs.iteratee.Error("Not Authorized",play.api.libs.iteratee.Input.Empty)
      result.fold(
        fail => concurrent.future { (foo,Enumerator.eof) },
        user => f(request)(user)
      )
    }
  }

  def currentUser(implicit request: RequestHeader, ec: ExecutionContext): Future[Option[USER]] = {
    inputValidator.validateInput(request).map { validation =>
      validation.fold(
        fail = { f => None },
        succ = { user => Some(user) }
      )
    }
  }
}

