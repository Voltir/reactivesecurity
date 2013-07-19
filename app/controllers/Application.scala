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

object InMemoryDemoUsers extends InMemoryUserService[DemoUser]

object AsID extends reactivesecurity.core.User.AsID[DemoUser] {
  def apply(idStr: String) = idStr
}

/*
trait DemoUsers extends RequiresUsers[DemoUser] {
  //TODO -- Shoot feet here... an InMemoryUserService must be a singleton or it will initilize multiple instances :/
  //what to do, what to do... cant really adopt the same solution that SecureSocial does, as I want to paramterize
  //on the user type... hrmmmmm
  override val users = InMemoryDemoUsers
  override def str2ID(inp: String) = inp
}
*/
class DemoAuthentication extends DefaultAuthentication[DemoUser](InMemoryDemoUsers) {
  override val asID = AsID
}

trait DemoSecured extends Controller with reactivesecurity.core.AsyncSecured[AnyContent,DemoUser] {
  override val inputValidator = new DemoAuthentication
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

object DemoProviders extends reactivesecurity.defaults.DefaultProviders[DemoUser] {
  override val userPasswordProvider = new DefaultUserPasswordProvider[DemoUser](InMemoryDemoUsers)
  override val asID = AsID
}

object DemoLogin extends reactivesecurity.defaults.DefaultLogin[DemoUser] {//with DemoUsers {
  override val users = InMemoryDemoUsers
  override val asID = AsID

  val todo_make_better = new DefaultHashValidator[DemoUser]

  def getLoginPage(request: RequestHeader): Html =
    views.html.login(loginForm)

  def getRegistrationStartPage(request: RequestHeader): Html =
    views.html.startRegistration(registrationForm)(request)

  def getRegistrationPage(request: RequestHeader, confirmation: String, registrationForm: Form[DemoUser]): Html =
    views.html.finishRegistration(registrationForm,confirmation)(request)

  def registrationStartRedirect: Call = routes.DemoLogin.startRegistration
  def registrationAfterRedirect: Call = routes.DemoLogin.login
  def registrationDoneRedirect: Call = routes.Application.foo

  def getUserForm(id: String) = { Form[DemoUser](
    mapping(
      "name" -> nonEmptyText,
      ("password" ->
        tuple(
          "password1" -> nonEmptyText,
          "password2" -> nonEmptyText
        ).verifying(Messages("passwords.dont.match"), passwords => passwords._1 == passwords._2)
        )
    )((name, password) => DemoUser(id,todo_make_better.hasher.hash(password._1)))(user => Some(user.id,("","")))
  )}

}