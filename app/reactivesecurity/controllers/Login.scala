package reactivesecurity.controllers

import play.api.mvc._
import play.api.templates.Html
import reactivesecurity.core.Authentication.LoginHandler

abstract class Login extends Controller {

  val loginHandler: LoginHandler[Request[AnyContent],Html]

  def login = Action { implicit request =>
    /*
    val to = ProviderController.landingUrl
    if ( SecureSocial.currentUser.isDefined ) {
      // if the user is already logged in just redirect to the app
      if ( Logger.isDebugEnabled() ) {
        Logger.debug("User already logged in, skipping login page. Redirecting to %s".format(to))
      }
      Redirect( to )
    } else {
      import com.typesafe.plugin._
      SecureSocial.withRefererAsOriginalUrl(Ok(use[TemplatesPlugin].getLoginPage(request, UsernamePasswordProvider.loginForm)))
    }
    */

    Ok(loginHandler.getLoginPage(request))
  }
}
