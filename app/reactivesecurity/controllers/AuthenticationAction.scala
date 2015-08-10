package reactivesecurity.controllers

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc.WebSocket.FrameFormatter
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.service.HasID
import scala.concurrent.{Future, ExecutionContext}

trait AuthenticationAction[User <: HasID] extends PlayAuthentication[RequestHeader,Result,User,AuthenticationFailure] {

  def ec: ExecutionContext

  case class AuthenticatedRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)

  case class MaybeAuthenticatedRequest[A](maybeUser: Option[User], request: Request[A]) extends WrappedRequest[A](request)

  case class Authenticated[A](action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[Result] = {
      authentication(user => action(new AuthenticatedRequest[A](user,request)))(request)
    }
    lazy val parser = action.parser
  }

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
      authentication(user => block(AuthenticatedRequest[A](user,request)))(request)
    }
    override def composeAction[A](action: Action[A]) = new Authenticated(action)
  }

  def AuthenticatedWS[A](f: RequestHeader => User => Future[(Iteratee[A, _], Enumerator[A])])(
    implicit frameFormatter: FrameFormatter[A]): WebSocket[A, A] = WebSocket.tryAccept[A] {
    request => validate(request).flatMap { result =>
      result.fold(
        fail => Future.successful(Left(play.api.mvc.Results.Unauthorized)),
        user => f(request)(user).map(s => Right(s))(ec)
      )
    }(ec)
  }

  object MaybeAuthenticated extends ActionBuilder[MaybeAuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: MaybeAuthenticatedRequest[A] => Future[Result]) = {
      validate(request).flatMap {
        case scala.util.Right(user) => block(MaybeAuthenticatedRequest(Some(user),request))
        case _ => block(MaybeAuthenticatedRequest(None,request))
      }(ec)
    }
  }

}

