package reactivesecurity.controllers

import play.api.mvc._

import reactivesecurity.core.Failures.ValidationFailure
import reactivesecurity.core.service.HasID
import scala.concurrent.{Future, ExecutionContext}

trait AuthorizedAction[User <: HasID] extends AuthenticationAction[User] {
  class AuthorizedRequest[A](user: User, request: Request[A]) extends AuthenticatedRequest[A](user,request)

  class AuthorizedActionBuilder(authorized: User => Future[Boolean]) extends ActionBuilder[AuthorizedRequest] {
    override def invokeBlock[A](request: Request[A], block: AuthorizedRequest[A] => Future[Result]) = {
      Authenticated.invokeBlock[A](request, { authenticated =>
        authorized(authenticated.user).flatMap { authorized =>
          if(authorized) block(new AuthorizedRequest[A](authenticated.user,request))
          else authFailureHandler.onAuthenticationFailure(request)(ValidationFailure)
        }(ec)
      })
    }
  }

  object Authorized {
    def apply[A](authorized: User => Future[Boolean]) = {
      new AuthorizedActionBuilder(authorized)
    }
  }
}
