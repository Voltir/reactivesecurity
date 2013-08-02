package controllers

import play.api.mvc._
import reactivesecurity.controllers.LoginForms._

import play.api.data.Form
import play.api.data.Forms._

import reactivesecurity.defaults.InMemoryUserService
import play.api.i18n.Messages
import defaults.InMemoryPasswordService //TODO Argh.. should be prefixed reactivesecurity

import reactivesecurity.core.providers.UserPasswordProvider

trait DemoAsID extends reactivesecurity.core.User.StringAsID[DemoUser] {
  override def strAsId(idStr: String) = idStr
}

object InMemoryDemoUsers extends InMemoryUserService[DemoUser] with DemoAsID { def idFromEmail(email: String) = email } //Todo

object InMemoryDemoPasswords extends InMemoryPasswordService[DemoUser]

object DemoProviders extends reactivesecurity.defaults.DefaultProviders[DemoUser] {
  override val userPasswordProvider = new UserPasswordProvider[DemoUser](InMemoryDemoUsers,InMemoryDemoPasswords)
}

object DemoLogin extends reactivesecurity.defaults.DefaultLogin[DemoUser] {
  override val userService = InMemoryDemoUsers
  override val passwordService = InMemoryDemoPasswords

  def onUnauthorized(request: RequestHeader): Result = Redirect(routes.DemoLogin.login)
  def onLoginSucceeded(request: RequestHeader): PlainResult = Redirect(routes.Application.foo)
  def onLogoutSucceeded(request: RequestHeader): PlainResult = Redirect(routes.DemoLogin.login)

  def onLogin(request: RequestHeader): Result =
    Ok(views.html.login(loginForm))

  def onStartSignUp(request: RequestHeader, error: Option[String]): Result =
    Ok(views.html.startRegistration(registrationForm)(request))

  def onFinishSignUp(request: RequestHeader): Result =
    Redirect(routes.DemoLogin.login).flashing("success" -> "TODO Email Sent")

  def onStartCompleteRegistration(request: RequestHeader, confirmation: String, id: String): Result =
    Ok(views.html.finishRegistration(getUserForm(id),confirmation)(request))

  def onCompleteRegistration[A](confirmation: String, id: String)(implicit request: Request[A]): (Option[DemoUser],Result) = {
    getUserForm(id).bindFromRequest().fold(
      errors => (None,Ok(views.html.finishRegistration(errors,confirmation))),
      user =>  (Some(user),Ok("2"))
    )
  }

  def getUserForm(id: String) = { Form[DemoUser](
    mapping(
      "name" -> nonEmptyText,
      "password" ->
        tuple(
          "password1" -> nonEmptyText,
          "password2" -> nonEmptyText
        ).verifying(Messages("passwords.dont.match"), passwords => passwords._1 == passwords._2)
    )((name, password) => DemoUser(id))(user => Some(user.id,("","")))
  )}
}
