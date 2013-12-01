package reactivesecurity.controllers

import reactivesecurity.core._
import reactivesecurity.core.providers._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.std.{OAuth2NoAccessCode, OauthNoVerifier, AuthenticationFailure}
import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.Password.PasswordService

import play.api.mvc._
import play.api.Logger
import play.api.cache.Cache
import play.api.mvc.Call
import play.api.libs.oauth.OAuth
import play.api.data._
import play.api.data.Forms._

import java.util.UUID
import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext, future}
import ExecutionContext.Implicits.global
import java.net.URLEncoder
import securesocial.core.providers.GoogleProvider
import core.util.RoutesHelper



abstract class Login[USER <: UsingID] extends Controller {
  val userService: UserService[USER]
  val passService: PasswordService[USER]

  val authenticator: Authenticator

  def onUnauthorized(request: RequestHeader): SimpleResult
  def onLoginSucceeded(request: RequestHeader): SimpleResult
  def onLogoutSucceeded(request: RequestHeader): SimpleResult
  def getLoginPage(request: RequestHeader): SimpleResult

  def userFromOauthData(todo: ThisDoesNotBelongHere): USER

  def login = Action { implicit request =>
    withRefererAsOriginalUrl(getLoginPage(request))
  }

  def logout = Action { implicit request =>
    authenticator.find(request).map { token =>
      authenticator.delete(token)
      onLogoutSucceeded(request).discardingCookies(DiscardingCookie(CookieParameters.cookieName))
    }.getOrElse(getLoginPage(request))
  }

  def authenticate(provider: String) = handleAuth(provider)
  def authenticateByPost(provider: String) = handleAuth(provider)

  def oauth1RetrieveRequestToken[A](service: OAuth, callbackUrl: String)(implicit request: Request[A]): SimpleResult = {
    import play.api.Play.current
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] callback url = " + callbackUrl)
    }
    service.retrieveRequestToken(callbackUrl) match {
      case Right(accessToken) =>  {
        val cacheKey = UUID.randomUUID().toString
        val redirect = Redirect(service.redirectUrl(accessToken.token)).withSession(request.session +
          (OAuth1Provider.CacheKey -> cacheKey))
        Cache.set(cacheKey, accessToken, 600) // set it for 10 minutes, plenty of time to log in
        redirect
      }
      case Left(e) => {
        Logger.error("[reactivesecurity] error retrieving request token", e)
        Ok("Todo -- Handle error case when retriving request token")
      }
    }
  }

  def oauth2RetrieveAccessCode[A](settings: OAuth2Settings, callbackUrl: String)(implicit request: Request[A]): SimpleResult = {
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
    //Redirect( url ).withSession(request.session + (IdentityProvider.SessionId, sessionId))
    Redirect( url ).withSession(request.session + ("sid", sessionId))
  }

  def handleOAuth1[A](p: OAuth1Provider[USER])(implicit request: Request[A]): Future[SimpleResult] = {
    val callback = RoutesHelper.authenticate(p.id).absoluteURL()
    handleGenericAuth(p) { fail => fail match {
      case _: OauthNoVerifier => p.maybeService.map {
        service => oauth1RetrieveRequestToken(service,callback)
        }.getOrElse {
          if ( Logger.isDebugEnabled ) {
            Logger.debug(s"[securesocial] Error using Oauth Service: ${p.id}")
          }
          onUnauthorized(request)
        }
      case _ => onUnauthorized(request)
    }}
  }

  def handleOAuth2[A](p: OAuth2Provider[USER])(implicit request: Request[A]): Future[SimpleResult] = {
    val callback = RoutesHelper.authenticate(p.id).absoluteURL()
    handleGenericAuth(p) { fail => fail match {
      case _: OAuth2NoAccessCode => p.maybeSettings.map {
        settings => oauth2RetrieveAccessCode(settings,callback)
      }.getOrElse(onUnauthorized(request))
      case _ => onUnauthorized(request)
    }}
  }

  def handleGenericAuth[A](p: AuthenticationService[Request[A],USER,AuthenticationFailure])(onFail: AuthenticationFailure => SimpleResult)(implicit request: Request[A]): Future[SimpleResult] = {
    p.authenticate(request).map { _.fold(
      fail => onFail(fail),
      (user: USER) => {
        //super hacks... really really want to fix this later...
        userService.save(user)
        completeAuthentication(user,session)
      }
    )}
  }

  lazy val userpass = new UserPasswordFormProvider(userService,passService)

  val oauth1providers = Map[String, OAuth1Provider[USER]](
    "linkedin" -> new LinkedInProviderMK2[USER](userFromOauthData)
  )

  val oauth2providers = Map[String,OAuth2Provider[USER]](
    "google" -> new GoogleProvider[USER](userFromOauthData)
  )

  private def handleAuth(provider: String) = Action.async { implicit request =>
    Logger.debug("[reactivesecurity] Authorizing with provider: "+provider)
    if(provider == "userpass") handleGenericAuth(userpass){ fail => onUnauthorized(request) }
    else {
      oauth1providers.get(provider).map { oauth1 =>
        handleOAuth1(oauth1)
      }.getOrElse { oauth2providers.get(provider).map { oauth2 =>
        handleOAuth2(oauth2)
      }.getOrElse(future { onUnauthorized(request) }) }
    }
  }

  def completeAuthentication(user: USER, session: Session)(implicit request: RequestHeader): SimpleResult = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] user logged in : [" + user + "]")
    }
    authenticator.create(user.id.toString) match {
      case Failure(_) => onUnauthorized(request)
      case Success(token) => onLoginSucceeded(request).withCookies(token.toCookie)
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