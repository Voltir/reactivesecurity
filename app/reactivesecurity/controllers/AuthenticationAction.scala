package reactivesecurity.controllers

import play.api.mvc._
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc.WebSocket.FrameFormatter
import reactivesecurity.core.Failures.AuthenticationFailure
import scala.concurrent.{Future, ExecutionContext}

trait AuthenticationAction[User] extends PlayAuthentication[RequestHeader,Result,User,AuthenticationFailure] {

  case class AuthenticatedRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)

  case class MaybeAuthenticatedRequest[A](maybeUser: Option[User], request: Request[A]) extends WrappedRequest[A](request)

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]) = {
      authentication(user => block(AuthenticatedRequest[A](user,request)))(executionContext)(request)
    }
  }

  object MaybeAuthenticated extends ActionBuilder[MaybeAuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: MaybeAuthenticatedRequest[A] => Future[Result]) = {
      validate(request).flatMap {
        case scala.util.Right(user) => block(MaybeAuthenticatedRequest(Some(user),request))
        case _ => block(MaybeAuthenticatedRequest(None,request))
      }(executionContext)
    }
  }

  def AuthenticatedWS[A]
      (f: RequestHeader => User => Future[(Iteratee[A, _], Enumerator[A])])
      (implicit frameFormatter: FrameFormatter[A], ec: ExecutionContext): WebSocket[A, A] = WebSocket.tryAccept[A] {
    request => validate(request).flatMap { result =>
      result.fold(
        fail => Future.successful(Left(play.api.mvc.Results.Unauthorized)),
        user => f(request)(user).map(s => Right(s))
      )
    }
  }
}

