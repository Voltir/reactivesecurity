package reactivesecurity.controllers

import play.api.mvc._
import play.api.Logger

import reactivesecurity.core.providers.{UserPasswordProvider,IdPass}
import reactivesecurity.core.User.{UsingID, IdFromString}
import reactivesecurity.core.std.AuthenticationServiceFailure
import reactivesecurity.core.{LoginHandler, Authenticator}

import scala.concurrent.{ExecutionContext, future}
import ExecutionContext.Implicits.global

import scalaz.{Failure,Success}


abstract class Providers[ID,USER <: UsingID[ID]] extends Controller {
  val str2id: IdFromString[ID]
  val userPasswordProvider: UserPasswordProvider[ID,USER]
  val authenticator: Authenticator

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
    println("AAAAAAAAAAAAAA authenticate -> handleAuth")
    val resultPromise = UserPasswordProvider.loginForm.bindFromRequest().fold(
      errors => future { Ok("Errors") },
      { case (id: String, password: String) => {
        val credentials = IdPass(str2id(id),password)
        userPasswordProvider.authenticate(credentials).map { result =>
          result match {
            case Failure(AuthenticationServiceFailure(f)) => Ok("Errors")
            case Failure(_) => Ok("Other Errors")
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
