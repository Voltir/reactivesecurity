package reactivesecurity.defaults

import reactivesecurity.core.LoginHandler
import play.api.mvc._
import play.api.templates.Html
import reactivesecurity.controllers.Login
import reactivesecurity.core.providers.UserPasswordProvider

object DefautLoginHandler extends LoginHandler[Request[AnyContent],Html] {
  def getLoginPage(request: Request[AnyContent]): Html = reactivesecurity.views.html.login(UserPasswordProvider.loginForm)
}

object DefaultLogin extends Login {
  val loginHandler = DefautLoginHandler
}
