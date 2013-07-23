package controllers


import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.templates.Html

import reactivesecurity.core.Authentication.AuthenticationFailureHandler
import reactivesecurity.core.std.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import reactivesecurity.controllers.LoginForms._
import reactivesecurity.defaults._
import play.api.mvc.Call
import reactivesecurity.core.PasswordInfo

//import controllers.DemoUser
import scala.Some


case class DemoUser(id: String, authenticationInfo: PasswordInfo) extends UsingID {
  type ID = String
}

object TodoAuthFailueHandler extends Controller with AuthenticationFailureHandler[Request[AnyContent],AuthenticationFailure,Result] {
  def onAuthenticationFailure(in: Request[AnyContent], failure: AuthenticationFailure): Result = {
    Ok("Auth Failed")
  }
}

trait DemoSecured extends Controller with reactivesecurity.core.AsyncSecured[AnyContent,DemoUser] {
  override val inputValidator = new DefaultAuthentication[DemoUser](InMemoryDemoUsers) { override val asID = AsID }
  override val authFailureHandler = TodoAuthFailueHandler
}

object Application extends DemoSecured {

  def index = AsyncSecuredAction(parse.anyContent) { case (request,user) =>
    Ok(user.id)
  }

  def foo = AsyncSecuredAction(parse.anyContent) { case (request,user) =>
    Ok("This is secure and you are: "+user.id)
  }

}



