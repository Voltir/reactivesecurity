package reactivesecurity.controllers

import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc.WebSocket.FrameFormatter

trait AuthenticationAction[USER <: UsingID] extends PlayAuthentication[RequestHeader,Result,USER,AuthenticationFailure] {

  implicit val ec: ExecutionContext


  case class AuthenticatedRequest[A](user: USER, request: Request[A]) extends WrappedRequest[A](request)

  case class MaybeAuthenticatedRequest[A](maybeUser: Option[USER], request: Request[A]) extends WrappedRequest[A](request)

  case class Authenticated[A](action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[Result] = {
      authentication(user => action(new AuthenticatedRequest[A](user,request)))(ec)(request)
    }
    lazy val parser = action.parser
  }

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
      authentication(user => block(AuthenticatedRequest[A](user,request)))(ec)(request)
    }
    override def composeAction[A](action: Action[A]) = new Authenticated(action)
  }

  def AuthenticatedWS[A](f: RequestHeader => USER => Future[(Iteratee[A, _], Enumerator[A])])(
    implicit frameFormatter: FrameFormatter[A]): WebSocket[A, A] = WebSocket.tryAccept[A] {
    request => validate(request).flatMap { result =>
      result.fold(
        fail => Future(Left(play.api.mvc.Results.Unauthorized)),
        user => f(request)(user).map(s => Right(s))
      )
    }
  }

  object MaybeAuthenticated extends ActionBuilder[MaybeAuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: MaybeAuthenticatedRequest[A] => Future[Result]) = {
      validate(request).flatMap {
        case scala.util.Right(user) => block(MaybeAuthenticatedRequest(Some(user),request))
        case _ => block(MaybeAuthenticatedRequest(None,request))
      }
    }
  }

}

