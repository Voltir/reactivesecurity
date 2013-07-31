package reactivesecurity.defaults

import reactivesecurity.core.LoginHandler
import play.api.mvc._
import play.api.templates.Html
import reactivesecurity.controllers.Login
import reactivesecurity.core.providers.UserPasswordProvider
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import reactivesecurity.core.User.{UserService, UsingID}



abstract class DefaultLogin[USER <: UsingID] extends Login[USER] {
  override val users: UserService[USER]
  override val confirmationTokenService = InMemoryConfirmationTokenService
}

/*
//TODO CAHHHHHHHHHHHHANGE LONGIN HANDLER OMG
object DefautLoginHandler extends LoginHandler[TodoUser] {

  def getLoginPage(request: RequestHeader): Html =
    reactivesecurity.views.html.login(UserPasswordProvider.loginForm)

  def getRegistrationStartPage(request: RequestHeader): Html =
    reactivesecurity.views.html.startRegistration(Login.registrationForm)(request)

  def getRegistrationPage(request: RequestHeader, confirmation: String): Html =
    reactivesecurity.views.html.finishRegistration(userForm,confirmation)(request)

  def registrationStartRedirect: Call =
    reactivesecurity.defaults.routes.DefaultLogin.startRegistration

  def registrationAfterRedirect: Call =
    reactivesecurity.defaults.routes.DefaultLogin.login

  def registrationDoneRedirect: Call =
    reactivesecurity.defaults.routes.DefaultLogin.login

  def userForm: Form[TodoUser] = DefaultLogin.userForm

}

object DefaultLogin extends Login[String,TodoUser] {

  val userEmail = "Wat@Wat.com"

  val userForm = Form[TodoUser](
    mapping(
      "name" -> nonEmptyText,
      ("password" ->
        tuple(
          "password1" -> nonEmptyText,
          "password2" -> nonEmptyText
        ).verifying(Messages("passwords.dont.match"), passwords => passwords._1 == passwords._2)
      )
    )
    ((name, password) => TodoUser(userEmail))
    (user => Some(user.id,("","")))
  )

  val loginHandler = DefautLoginHandler
  val userService = DefaultUserService
  val confirmationTokenService = InMemoryConfirmationTokenService
  val asId = StringFromString
}
*/