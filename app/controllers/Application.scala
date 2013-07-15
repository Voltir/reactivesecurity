package controllers

import scalaz.{Failure, Success, Validation}
import play.api.mvc._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import reactivesecurity.core.Authentication.{AuthenticationFailureHandler, InputValidator}
import reactivesecurity.core.std.AuthenticationFailure


case class DemoUser(id: String, password: String)

object FooInputValidator extends InputValidator[Request[AnyContent],DemoUser,AuthenticationFailure] {
  def validateInput(in: Request[AnyContent]): Validation[AuthenticationFailure,DemoUser] = {
    Success(DemoUser("Bob","Password"))
  }
}

object FooAuthFailueHandler extends Controller with AuthenticationFailureHandler[Request[AnyContent],AuthenticationFailure,Result] {
  def onAuthenticationFailure(in: Request[AnyContent], failure: AuthenticationFailure): Result = {
    Ok("Auth Failed")
  }
}

trait DemoSecured extends Controller with reactivesecurity.core.Secured[AnyContent,DemoUser] {
  override val inputValidator = FooInputValidator
  override val authFailureHandler = FooAuthFailueHandler
}

object Application extends DemoSecured {

  def index = AsyncSecuredAction(parse.anyContent) { case (request,user) =>
    Ok(user.id)
  }
  
}