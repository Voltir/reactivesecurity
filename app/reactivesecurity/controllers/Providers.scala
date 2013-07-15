package reactivesecurity.controllers

import play.api.mvc._
import reactivesecurity.core.providers.{IdPass, UserPasswordProvider}
import scalaz.Failure
import scalaz.Success
import reactivesecurity.core.std.AuthenticationServiceFailure
import reactivesecurity.core.User.{UserWithIdentity, IdService}
import scala.concurrent.{ExecutionContext, future}
import ExecutionContext.Implicits.global
import reactivesecurity.core.providers.IdPass
import scalaz.Success
import reactivesecurity.core.std.AuthenticationServiceFailure
import scalaz.Failure
import play.api.Logger
import reactivesecurity.core.Authenticator

abstract class Providers[ID, USER >: UserWithIdentity[ID]] extends Controller {
  val idService: IdService[ID]
  val userPasswordProvider: UserPasswordProvider[ID,USER]
  val authenticator: Authenticator

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
    val resultPromise = UserPasswordProvider.loginForm.bindFromRequest().fold(
      errors => future { Ok("Errors") },
      { case (id: String, password: String) => {
        val credentials = IdPass(idService.idFromString(id),password)
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

  def completeAuthentication(user: UserWithIdentity[ID], session: Session)(implicit request: RequestHeader): Result = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] user logged in : [" + user + "]")
    }
    //TODO val withSession = Events.fire(new LoginEvent(user)).getOrElse(session)
    authenticator.create(user.rawId) match {
      case Failure(_) => Ok("TODO")
      case Success(token) => {
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
    Ok("TODO")
  }
}
