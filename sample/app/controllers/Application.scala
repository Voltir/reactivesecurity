package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc._

import reactivesecurity.core.Authentication.AuthenticationFailureHandler
import reactivesecurity.core.std.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import reactivesecurity.defaults._
import play.api.templates.Html


case class DemoUser(id: String) extends UsingID {
  type ID = String
}

object TodoAuthFailueHandler extends Controller with AuthenticationFailureHandler[RequestHeader,AuthenticationFailure,Result] {
  def onAuthenticationFailure(in: RequestHeader, failure: AuthenticationFailure): Result = {
    Ok("Auth Failed")
  }
}

trait DemoSecured extends Controller with reactivesecurity.core.AsyncSecured[DemoUser] {
  override val inputValidator = new DefaultAuthentication[DemoUser](InMemoryDemoUsers) { }
  override val authFailureHandler = TodoAuthFailueHandler
}

object Application extends DemoSecured {

  def index = Secured { implicit request => user =>
    Ok(user.id)
  }

  def foo = Secured { implicit request => user =>
    Ok("This is secure and you are: "+user.id)
  }

  def todoDeleteThis = Secured { implicit request => user =>
    Some(2).fold(BadRequest(Html("???")))(i => Ok(Html("Wat")))
  }

}



