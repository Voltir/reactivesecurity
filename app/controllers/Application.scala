package controllers

import scalaz.{Failure, Success, Validation}
import play.api.mvc._
import scala.concurrent.{future,Future,ExecutionContext}
import ExecutionContext.Implicits.global

import reactivesecurity.core.Authentication.{AsyncInputValidator, AuthenticationFailureHandler}
import reactivesecurity.core.std.AuthenticationFailure
import reactivesecurity.core.User.UserWithIdentity
import reactivesecurity.defaults.DefaultInputValidator


case class DemoUser(id: String, password: String) extends UserWithIdentity[String] {
  def rawId = id
}

object FooInputValidator extends AsyncInputValidator[Request[AnyContent],String,DemoUser,AuthenticationFailure] {
  def validateInput(in: Request[AnyContent])(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,DemoUser]] = {
    future { Success(DemoUser("Bob","Password")) }
  }
}

object FooAuthFailueHandler extends Controller with AuthenticationFailureHandler[Request[AnyContent],AuthenticationFailure,Result] {
  def onAuthenticationFailure(in: Request[AnyContent], failure: AuthenticationFailure): Result = {
    Ok("Auth Failed")
  }
}

trait DemoSecured extends Controller with reactivesecurity.core.AsyncSecured[AnyContent,String,DemoUser] {
  override val inputValidator = new DefaultInputValidator[DemoUser]
  override val authFailureHandler = FooAuthFailueHandler
}

object Application extends DemoSecured {

  def index = AsyncSecuredAction(parse.anyContent) { case (request,user) =>
    Ok(user.id)
  }

  def foo = AsyncSecuredAction(parse.anyContent) { case (request,user) =>
    Ok("This is secure and you are: "+user.id)
  }
  
}