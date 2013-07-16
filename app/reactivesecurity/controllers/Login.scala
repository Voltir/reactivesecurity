package reactivesecurity.controllers

import scalaz._
import scalaz.Scalaz._

import play.api.mvc._
import play.api.Logger

import play.api.templates.Html
import reactivesecurity.core.LoginHandler
import play.api.http.HeaderNames

abstract class Login extends Controller {

  val loginHandler: LoginHandler[Request[AnyContent],Html]

  def login = Action { implicit request =>
    println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAaAAA -- login")
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

    withRefererAsOriginalUrl(Ok(loginHandler.getLoginPage(request)))
  }

  def withRefererAsOriginalUrl[A](result: Result)(implicit request: Request[A]): Result = {
    request.session.get(LoginHandler.OriginalUrlKey) match {
      // If there's already an original url recorded we keep it: e.g. if s.o. goes to
      // login, switches to signup and goes back to login we want to keep the first referer
      case Some(_) => result
      case None => {
        request.headers.get(HeaderNames.REFERER).map { referer =>
        // we don't want to use the ful referer, as then we might redirect from https
        // back to http and loose our session. So let's get the path and query string only
          val idxFirstSlash = referer.indexOf("/", "https://".length())
          val refererUri = if (idxFirstSlash < 0) "/" else referer.substring(idxFirstSlash)
          result.withSession(
            request.session + (LoginHandler.OriginalUrlKey -> refererUri))
        } | result
      }
    }
  }

}
