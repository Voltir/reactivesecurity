package controllers

import scalaz.{Failure, Success, Validation}
import play.api.mvc._
import scala.concurrent.{future,Future,ExecutionContext}
import ExecutionContext.Implicits.global

import reactivesecurity.core.Authentication.{AsyncInputValidator, AuthenticationFailureHandler}
import reactivesecurity.core.std.{ValidationFailure, UserServiceFailure, AuthenticationFailure}
import reactivesecurity.core.User.{RequiresUsers, UsingID}
import reactivesecurity.controllers.LoginForms._
import reactivesecurity.defaults.{DefaultUserPasswordProvider, InMemoryUserService, DefaultLogin, DefaultInputValidator}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import reactivesecurity.core.LoginHandler
import reactivesecurity.controllers.ConfirmationTokenService
import play.api.templates.Html


case class DemoUser(id: String, password: String) extends UsingID[String]

/*
object FooInputValidator extends AsyncInputValidator[Request[AnyContent],DemoUser,AuthenticationFailure] {
  def validateInput(in: Request[AnyContent])(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,DemoUser]] = {
    future { Success(DemoUser("Bob","Password")) }
  }
}


object FooAuthFailueHandler extends Controller with AuthenticationFailureHandler[Request[AnyContent],AuthenticationFailure,Result] {
  def onAuthenticationFailure(in: Request[AnyContent], failure: AuthenticationFailure): Result = {
    Ok("Auth Failed")
  }
}

trait DemoSecured extends Controller with reactivesecurity.core.AsyncSecured[AnyContent,DemoUser] {
  override val inputValidator = new DefaultInputValidator[DemoUser]
  override val authFailureHandler = FooAuthFailueHandler
}
*/
/*
object DemoLogin extends DefaultLogin[String,DemoUser] {
  override val asId = StringFromString
  override val userService = DefaultUserService
  override val confirmationTokenService = InMemoryConfirmationTokenService

  def getLoginPage(request: RequestHeader): Html =
    views.html.login(UserPasswordProvider.loginForm)

  def getRegistrationStartPage(request: RequestHeader): Html =
    views.html.startRegistration(Login.registrationForm)(request)

  def getRegistrationPage(request: RequestHeader, confirmation: String): Html =
    views.html.finishRegistration(userForm,confirmation)(request)

  /*
  def registrationStartRedirect: Call =
    routes.DefaultLogin.startRegistration

  def registrationAfterRedirect: Call =
    reactivesecurity.defaults.routes.DefaultLogin.login

  def registrationDoneRedirect: Call =
    reactivesecurity.defaults.routes.DefaultLogin.login
  */

  val userEmail = "Wat@Wat.com"

  val userForm = Form[DemoUser](
    mapping(
      "name" -> nonEmptyText,
      ("password" ->
        tuple(
          "password1" -> nonEmptyText,
          "password2" -> nonEmptyText
        ).verifying(Messages("passwords.dont.match"), passwords => passwords._1 == passwords._2)
        )
    )
      ((name, password) => DemoUser(userEmail,password))
      (user => Some(user.id,("","")))
  )

}
*/

object TodoAuthFailueHandler extends Controller with AuthenticationFailureHandler[Request[AnyContent],AuthenticationFailure,Result] {
  def onAuthenticationFailure(in: Request[AnyContent], failure: AuthenticationFailure): Result = {
    Ok("Auth Failed")
  }
}

trait DemoUsers extends RequiresUsers[String,DemoUser] {
  override val users = new InMemoryUserService[String,DemoUser]
  def string2Id(inp: String) = inp
}

trait DemoSecured extends Controller with reactivesecurity.core.AsyncSecured[AnyContent,DemoUser] {
  override val inputValidator = new DefaultInputValidator[String,DemoUser] with DemoUsers
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

object DemoProviders extends reactivesecurity.defaults.DefaultProviders[String,DemoUser] {
  override val userPasswordProvider = new DefaultUserPasswordProvider[String,DemoUser] with DemoUsers
}

object DemoLogin extends reactivesecurity.defaults.DefaultLogin[String,DemoUser] with DemoUsers {

  def getLoginPage(request: RequestHeader): Html =
    views.html.login(loginForm)

  def getRegistrationStartPage(request: RequestHeader): Html =
    views.html.startRegistration(registrationForm)(request)

  def getRegistrationPage(request: RequestHeader, confirmation: String): Html =
    views.html.finishRegistration(userForm,confirmation)(request)


  def registrationStartRedirect: Call = routes.DemoLogin.startRegistration

  def registrationAfterRedirect: Call = routes.DemoLogin.login

  def registrationDoneRedirect: Call = routes.DemoLogin.login

  val userEmail = "Wat@Wat.com"

  val userForm = Form[DemoUser](
    mapping(
      "name" -> nonEmptyText,
      ("password" ->
        tuple(
          "password1" -> nonEmptyText,
          "password2" -> nonEmptyText
        ).verifying(Messages("passwords.dont.match"), passwords => passwords._1 == passwords._2)
        )
    )
      ((name, password) => DemoUser(userEmail,password._1))
      (user => Some(user.id,("","")))
  )

}