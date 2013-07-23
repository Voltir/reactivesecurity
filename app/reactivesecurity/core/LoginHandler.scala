package reactivesecurity.core

import play.api.Play
import play.api.mvc.{Call, RequestHeader}
import play.api.templates.Html
import reactivesecurity.core.User.UsingID
import play.api.data.Form

trait LoginHandler[USER <: UsingID] {
  def getLoginPage(request: RequestHeader): Html
  def getRegistrationStartPage(request: RequestHeader): Html
  def getRegistrationPage(request: RequestHeader, confirmation: String, registrationForm: Form[USER]): Html

  def registrationStartRedirect: Call
  def registrationAfterRedirect: Call
  def registrationDoneRedirect: Call

  def getUserForm(id: USER#ID): Form[USER]
}


object LoginHandler {

  val onLoginGoTo = "reactivesecurity.onLoginGoTo"

  def landingUrl = Play.current.configuration.getString(onLoginGoTo).getOrElse(
    Play.current.configuration.getString("application.context").getOrElse("/"))

  val OriginalUrlKey = "original-url"
}

