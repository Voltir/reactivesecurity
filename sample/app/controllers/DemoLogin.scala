package controllers

import play.api.mvc._
import play.api.mvc.Results._
import play.api.templates.Html
import reactivesecurity.controllers.LoginForms._

import play.api.data.Form
import play.api.data.Forms._

import reactivesecurity.defaults.{BCryptUserPasswordProvider, InMemoryUserService, BCryptHasher}
import play.api.i18n.Messages

object InMemoryDemoUsers extends InMemoryUserService[DemoUser]

object AsID extends reactivesecurity.core.User.AsID[DemoUser] {
  def apply(idStr: String) = idStr
}


object DemoProviders extends reactivesecurity.defaults.DefaultProviders[DemoUser] {
  override val userPasswordProvider = new BCryptUserPasswordProvider[DemoUser](InMemoryDemoUsers)
  override val asID = AsID
}

object DemoLogin extends reactivesecurity.defaults.DefaultLogin[DemoUser] {
  override val users = InMemoryDemoUsers
  override val asID = AsID

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
    )((name, password) => DemoUser(id,BCryptHasher.hash(password._1)))(user => Some(user.id,("","")))
  )}
}
