package reactivesecurity.controllers

import play.api.mvc._
import play.api.Logger

import reactivesecurity.core.providers.{UserPasswordProvider,IdPass}
import reactivesecurity.core.User.{AsID, UsingID}
import reactivesecurity.core.std.AuthenticationServiceFailure
import reactivesecurity.core.{LoginHandler, Authenticator}

import scala.concurrent.{ExecutionContext, future}
import ExecutionContext.Implicits.global

import scalaz.{Failure,Success}


abstract class Providers[USER <: UsingID] extends Controller {
  val userPasswordProvider: UserPasswordProvider[USER]
  val authenticator: Authenticator
  val asID: AsID[USER]

  def toUrl(implicit request: RequestHeader) = session.get(LoginHandler.OriginalUrlKey).getOrElse(LoginHandler.landingUrl)

  def authenticate(provider: String) = handleAuth(provider)
  def authenticateByPost(provider: String) = handleAuth(provider)

  private def handleAuth(provider: String) = Action { implicit request =>
    /*
    Registry.providers.get(provider) match {
      case Some(p) => {
        try {
          p.authenticate().fold( result => result , {
            user => completeAuthentication(user, session)
          })
        } catch {
          case ex: AccessDeniedException => {
            Redirect(RoutesHelper.login()).flashing("error" -> Messages("securesocial.login.accessDenied"))
          }

          case other: Throwable => {
            Logger.error("Unable to log user in. An exception was thrown", other)
            Redirect(RoutesHelper.login()).flashing("error" -> Messages("securesocial.login.errorLoggingIn"))
          }
        }
      }
      case _ => NotFound
    }
    */
    val resultPromise = LoginForms.loginForm.bindFromRequest().fold(
      errors => future { Ok("Errors") },
      { case (id: String, password: String) => {
        val credentials = IdPass(asID(id),password)
        userPasswordProvider.authenticate(credentials).map { result =>
          result match {
            case Failure(AuthenticationServiceFailure(f)) => Ok(f.toString)
            case Failure(wat) => Ok("Other Errors: " + wat)
            case Success(user) => completeAuthentication(user,session)
          }
        }
      }}
    )
    Async {
      resultPromise
    }
  }

  def completeAuthentication(user: USER, session: Session)(implicit request: RequestHeader): Result = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] user logged in : [" + user + "]")
    }
    //TODO val withSession = Events.fire(new LoginEvent(user)).getOrElse(session)
    authenticator.create(user.id.toString) match {
      case Failure(_) => Ok("TODO -- ERROR completeAuthentication")
      case Success(token) => {
        println("Doing redirect "+toUrl)
        println("Cookie: "+token.toCookie)
        Redirect(toUrl).withCookies(token.toCookie)
      }
    }
    /*
    Authenticator.create(user) match {
      case Right(authenticator) => {
        Redirect(toUrl).withSession(withSession -
          SecureSocial.OriginalUrlKey -
          IdentityProvider.SessionId -
          OAuth1Provider.CacheKey).withCookies(authenticator.toCookie)
      }
      case Left(error) => {
        // improve this
        throw new RuntimeException("Error creating authenticator")
      }
    }
    */
  }
}