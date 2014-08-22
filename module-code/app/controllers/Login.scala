package reactivesecurity.controllers

import reactivesecurity.core._
import reactivesecurity.core.providers._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.Failures._
import reactivesecurity.core.Password.PasswordService

import play.api.mvc._
import play.api.Logger
import play.api.cache.Cache
import play.api.libs.oauth.OAuth
import play.api.data._
import play.api.data.Forms._

import java.util.UUID
import reactivesecurity.core.util.OauthUserData
import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import java.net.URLEncoder
import securesocial.core.providers.GoogleProvider
import core.util.RoutesHelper

sealed trait AssociateProviderResult[USER <: UsingID]
case class AssociateSuccessful[USER <: UsingID](uid: USER#ID, oauth: OauthUserData) extends AssociateProviderResult[USER]
case class AssociateAlreadyInUse[USER <: UsingID](user: USER) extends AssociateProviderResult[USER]
case class AssociateFailed[USER <: UsingID](wat: String) extends AssociateProviderResult[USER]

abstract class Login[USER <: UsingID] extends Controller with AuthenticationAction[USER] {
  val userService: UserService[USER]
  val passService: PasswordService[USER]
  val authenticator: Authenticator[USER]

  val secured = play.api.Play.current.configuration.getString("https.port").isDefined

  def onUnauthorized(implicit request: RequestHeader): Result

  def onLoginSucceeded(provider: String)(implicit request: RequestHeader): Result

  def onLogoutSucceeded(implicit request: RequestHeader): Result

  def onStillLoggedIn(implicit request: RequestHeader): Result

  def getLoginPage(implicit request: RequestHeader): Result

  def onNewOauthUser(data: reactivesecurity.core.util.OauthUserData)(implicit request: RequestHeader): Result

  def login = MaybeAuthenticated { implicit request =>
    request.maybeUser.map { user =>
      onStillLoggedIn(request)
    } getOrElse {
      withRefererAsOriginalUrl(getLoginPage(request))
    }
  }

  def logout = Action.async { implicit request =>
    authenticator.touch(request).map { _.map { token =>
      authenticator.delete(token)
      onLogoutSucceeded(request).discardingCookies(DiscardingCookie(CookieParameters.cookieName))
    }.getOrElse {
      getLoginPage(request)
    }}
  }

  lazy val providers: Map[String,Provider[USER]] = Map(
    "userpass" -> UserPassword(userService,passService),
    "linkedin" -> LinkedInProvider[USER](userService),
    "twitter" -> TwitterProvider[USER](userService),
    "google" -> GoogleProvider[USER](userService)
  )

  private def failure(implicit request: RequestHeader) = Future(onUnauthorized(request))

  def associateProvider(provider: String) = Authenticated { implicit request =>
    import play.api.Play.current
    val callback = RoutesHelper.associateProviderCallback(provider)
    val associateKey = UUID.randomUUID().toString
    Cache.set(associateKey,request.user.id,600)
    providers.get(provider).flatMap {
      //TODO -- in this case I think we can redirect directly to the associateProviderCallback and assume the proper form was set
      //allowing the UserPassword.authenticate bindFromRequest succeed
      //case p: UserPassword[USER] => handleGenericAuth(p)(fail => onUnauthorized(request))
      case p: OAuth1Provider[USER] => p.maybeService.map { oauth =>
        oauth1RetrieveAccessToken(oauth,callback.absoluteURL(secured)).flashing("associate-key"->associateKey)
      }

      case p: OAuth2Provider[USER] => p.maybeSettings.map { settings =>
        oauth2RetrieveAccessCode(settings,callback.absoluteURL(secured)).flashing("associate-key"->associateKey)
      }

      case _ => Some(onUnauthorized)
    } getOrElse {
      onUnauthorized
    }
  }

  def associateProviderCallback(provider: String)(f: RequestHeader => AssociateProviderResult[USER] => Future[Result])= Action.async { implicit request =>
    import play.api.Play.current
    val savedUid = request.flash.get("associate-key").flatMap(Cache.get)
    providers.get(provider).map { p =>
      p.authenticate(request).flatMap {
        case Success(user) => f(request)(AssociateAlreadyInUse[USER](user))
        case Failure(RequiresNewOauthUser(oauth)) => {
          savedUid.map { uid =>
            f(request)(AssociateSuccessful[USER](uid.asInstanceOf[USER#ID],oauth))
          } getOrElse {
            f(request)(AssociateFailed("Error getting stored UID from Cache"))
          }
        }
        case Failure(fail) => { f(request)(AssociateFailed(s"Authentication failure: ${fail}")) }
      }
    } getOrElse {
      Future(BadRequest)
    }
  }

  def authenticate(provider: String) = handleAuth(provider)
  def authenticateByPost(provider: String) = handleAuth(provider)

  def oauth1RetrieveAccessToken[A](service: OAuth, callbackUrl: String)(implicit request: Request[A]): Result = {
    import play.api.Play.current
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] callback url = " + callbackUrl)
    }
    service.retrieveRequestToken(callbackUrl) match {
      case Right(requestToken) =>  {
        val cacheKey = UUID.randomUUID().toString
        val redirect = Redirect(service.redirectUrl(requestToken.token)).withSession(request.session + (OAuth1Provider.CacheKey -> cacheKey))
        Cache.set(cacheKey, requestToken, 600) // set it for 10 minutes, plenty of time to log in
        redirect
      }
      case Left(e) => {
        Logger.error("[reactivesecurity] error retrieving request token", e)
        BadRequest
      }
    }
  }

  def oauth2RetrieveAccessCode[A](settings: OAuth2Settings, callbackUrl: String)(implicit request: Request[A]): Result = {
    import play.api.Play.current
    // There's no code in the request, this is the first step in the oauth flow
    val state = UUID.randomUUID().toString
    val sessionId = request.session.get("sid").getOrElse(UUID.randomUUID().toString)
    Cache.set(sessionId, state)
    var params = List(
      (OAuth2Constants.ClientId, settings.clientId),
      (OAuth2Constants.RedirectUri, callbackUrl),
      (OAuth2Constants.ResponseType, OAuth2Constants.Code),
      (OAuth2Constants.State, state))
    settings.scope.foreach( s => { params = (OAuth2Constants.Scope, s) :: params })
    val url = settings.authorizationUrl +
      params.map( p => p._1 + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("?", "&", "")
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] authorizationUrl = %s".format(settings.authorizationUrl))
      Logger.debug("[reactivesecurity] redirecting to: [%s]".format(url))
    }
    Redirect(url).withSession(request.session + ("sid", sessionId))
  }

  def handleOAuth1[A](p: OAuth1Provider[USER])(implicit request: Request[A]): Future[Result] = {
    val callback = RoutesHelper.authenticate(p.providerId).absoluteURL(secured)
    handleGenericAuth(p) {
      case OauthNoVerifier => p.maybeService.map { service =>
        oauth1RetrieveAccessToken(service,callback)
      }.getOrElse {
        if ( Logger.isDebugEnabled ) {
          Logger.debug(s"[reactivesecurity] Error using Oauth Service: ${p.providerId}")
        }
        onUnauthorized(request)
      }
      case _ => onUnauthorized(request)
    }
  }

  def handleOAuth2[A](p: OAuth2Provider[USER])(implicit request: Request[A]): Future[Result] = {
    val callback = RoutesHelper.authenticate(p.providerId).absoluteURL(secured)
    handleGenericAuth(p) {
      case OAuth2NoAccessCode => p.maybeSettings.map {
        settings => oauth2RetrieveAccessCode(settings,callback)
      }.getOrElse(onUnauthorized(request))
      case _ => onUnauthorized(request)
    }
  }

  def handleGenericAuth[A](p: Provider[USER])(onFail: AuthenticationFailure => Result)(implicit request: Request[A]): Future[Result] = {
    p.authenticate(request).flatMap {
      case Success(user) => completeAuthentication(user)(onLoginSucceeded(p.providerId))
      case Failure(RequiresNewOauthUser(oauth)) => Future(onNewOauthUser(oauth))
      case Failure(f) => Future(onFail(f))
    }
  }

  private def handleAuth(provider: String) = Action.async { implicit request =>
    Logger.debug("[reactivesecurity] Authorizing with provider: "+provider)
    providers.get(provider).map {
      case p: UserPassword[USER] => handleGenericAuth(p)(fail => onUnauthorized(request))
      case p: OAuth1Provider[USER] => handleOAuth1(p)
      case p: OAuth2Provider[USER] => handleOAuth2(p)
      case _ => failure
    } getOrElse {
      failure
    }
  }

  def completeAuthentication(user: USER)(onSuccess: Result)(implicit request: RequestHeader): Future[Result] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] user logged in : [" + user + "]")
    }

    val expire = org.joda.time.Duration.standardHours(12)
    authenticator.create(user.id, expire).map {
      case Failure(_) => onUnauthorized(request)
      case Success(token) => onSuccess.withCookies(authenticator.cookies(token,secured))
    }
  }
}

object LoginForm {
  val loginForm = Form[(String,String)](
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )
}