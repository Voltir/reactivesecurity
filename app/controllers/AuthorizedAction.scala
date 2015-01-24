package reactivesecurity.controllers

import reactivesecurity.core.Authentication
import reactivesecurity.core.Failures.ValidationFailure
import scala.concurrent.{Future, ExecutionContext}

import play.api.mvc._

import reactivesecurity.core.User.UsingID
import core.Authorization.AuthorizationService

trait AuthorizedAction[USER <: UsingID] extends AuthenticationAction[USER] {
  import ExecutionContext.Implicits.global

  class AuthorizedRequest[A](user: USER, request: Request[A]) extends AuthenticatedRequest[A](user,request)

  class AuthorizedActionBuilder(authorized: USER => Future[Boolean]) extends ActionBuilder[AuthorizedRequest] {
    override def invokeBlock[A](request: Request[A], block: AuthorizedRequest[A] => Future[Result]) = {
      val ec = implicitly[ExecutionContext]
      Authenticated.invokeBlock[A](request, { authenticated =>
        authorized(authenticated.user).flatMap { authorized =>
          if(authorized) block(new AuthorizedRequest[A](authenticated.user,request))
          else authFailureHandler.onAuthenticationFailure(request)(ValidationFailure)
        }(ec)
      })
    }
  }

  object Authorized {
    def apply[A](authorized: USER => Future[Boolean]) = {
      new AuthorizedActionBuilder(authorized)
    }
  }
}
