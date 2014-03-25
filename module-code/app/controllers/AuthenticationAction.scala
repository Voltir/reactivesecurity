package reactivesecurity.controllers

import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc.WebSocket.FrameFormatter
import scalaz.Success
import controllers.PlayAuthentication

trait AuthenticationAction[USER <: UsingID] extends PlayAuthentication[RequestHeader,SimpleResult,USER,AuthenticationFailure] {
  import ExecutionContext.Implicits.global

  private def handleStatus(fuck: Int)(result: SimpleResult) = {
    if(fuck > 0) result.withCookies() else result
  }

  case class AuthenticatedRequest[A](val user: USER, request: Request[A]) extends WrappedRequest[A](request)

  case class MaybeAuthenticatedRequest[A](val maybeUser: Option[USER], request: Request[A]) extends WrappedRequest[A](request)

  case class Authenticated[A](action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[SimpleResult] = {
      /*inputValidator.validateInput(request).flatMap { validation =>
        validation.fold(
          fail = { f => authFailureHandler.onAuthenticationFailure(request,f) },
          succ = { user => action(new AuthenticatedRequest[A](user,request)) }
        )
      }
      */
      val ec = implicitly[ExecutionContext]
      authentication(user => action(new AuthenticatedRequest[A](user,request)))(ec)(request).map {
        case (stab,die) => handleStatus(stab)(die)
      }
    }
    lazy val parser = action.parser
  }

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
      val ec = implicitly[ExecutionContext]
      authentication(user => block(new AuthenticatedRequest[A](user,request)))(ec)(request).map {
        case (stab,die) => handleStatus(stab)(die)
      }
    }
    override def composeAction[A](action: Action[A]) = new Authenticated(action)
  }

  /*
  def AuthenticatedWS[A](f: RequestHeader => USER => Future[(Iteratee[A, _], Enumerator[A])])(implicit frameFormatter: FrameFormatter[A]): WebSocket[A] = WebSocket.async[A] {
    request => inputValidator.validateInput(request).flatMap { result =>
      val foo: Iteratee[A,Nothing] = play.api.libs.iteratee.Error("Not Authorized",play.api.libs.iteratee.Input.Empty)
      result.fold(
        fail => concurrent.future { (foo,Enumerator.eof) },
        user => f(request)(user)
      )
    }
  }
  */


  object MaybeAuthenticated extends ActionBuilder[MaybeAuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: MaybeAuthenticatedRequest[A] => Future[SimpleResult]) = {
      inputValidator.validateInput(request).flatMap {
        case (status,Success(user)) => block(MaybeAuthenticatedRequest(Some(user),request)).map(a => handleStatus(status)(a))
        case _ => block(MaybeAuthenticatedRequest(None,request))
      }
    }
  }

}

