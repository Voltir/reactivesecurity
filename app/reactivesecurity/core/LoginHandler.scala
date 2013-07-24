package reactivesecurity.core

import play.api.Play
import play.api.mvc._

import reactivesecurity.core.User.UsingID


/*
trait LoginHandler[USER <: UsingID] {
  def getLoginPage(request: RequestHeader): Html
  def getRegistrationStartPage(request: RequestHeader): Html
  def getRegistrationPage(request: RequestHeader, confirmation: String, registrationForm: Form[USER]): Html

  def registrationStartRedirect: Call
  def registrationAfterRedirect: Call
  def registrationDoneRedirect: Call

  def getUserForm(id: USER#ID): Form[USER]
}
*/
/*
trait LoginHandler[USER <: UsingID] {
  def onUnauthorized(request: RequestHeader): Result
  def onLoginSucceeded(request: RequestHeader): PlainResult
  def onLogoutSucceeded(request: RequestHeader): PlainResult
  def onLogin(request: RequestHeader): Result

  def onStartSignUp(request: RequestHeader, error: Option[String]): Result
  def onFinishSignUp(request: RequestHeader): Result
  def onStartCompleteRegistration(request: RequestHeader, confirmation: String, id: USER#ID): Result
  def onCompleteRegistration[A](id: USER#ID)(store: USER => Unit)(implicit request: Request[A]): Result
}
*/

object LoginHandler {

  val onLoginGoTo = "reactivesecurity.onLoginGoTo"

  def landingUrl = Play.current.configuration.getString(onLoginGoTo).getOrElse(
    Play.current.configuration.getString("application.context").getOrElse("/"))

  val OriginalUrlKey = "original-url"
}

