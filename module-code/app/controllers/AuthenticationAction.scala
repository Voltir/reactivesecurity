package reactivesecurity.controllers

import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.Authentication.AsyncAuthentication
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc.WebSocket.FrameFormatter

trait AuthenticationAction[USER <: UsingID] extends AsyncAuthentication[RequestHeader,SimpleResult,USER,AuthenticationFailure] {
  import ExecutionContext.Implicits.global

  class AuthenticatedRequest[A](val user: USER, request: Request[A]) extends WrappedRequest[A](request)

  case class Authenticated[A](action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[SimpleResult] = {
      inputValidator.validateInput(request).flatMap { validation =>
        validation.fold(
          fail = { f => authFailureHandler.onAuthenticationFailure(request,f) },
          succ = { user => action(new AuthenticatedRequest[A](user,request)) }
        )
      }
    }
    lazy val parser = action.parser
  }

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
      val ec = implicitly[ExecutionContext]
      authentication(user => block(new AuthenticatedRequest[A](user,request)))(ec)(request)
    }
    override def composeAction[A](action: Action[A]) = new Authenticated(action)
  }

  def AuthenticatedWS[A](f: RequestHeader => USER => Future[(Iteratee[A, _], Enumerator[A])])(implicit frameFormatter: FrameFormatter[A]): WebSocket[A] = WebSocket.async[A] {
    request => inputValidator.validateInput(request).flatMap { result =>
      val foo: Iteratee[A,Nothing] = play.api.libs.iteratee.Error("Not Authorized",play.api.libs.iteratee.Input.Empty)
      result.fold(
        fail => concurrent.future { (foo,Enumerator.eof) },
        user => f(request)(user)
      )
    }
  }
}

