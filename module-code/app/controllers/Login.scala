package reactivesecurity.controllers

import reactivesecurity.core._
import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.providers._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.Failures._
import reactivesecurity.core.Password.PasswordService

import play.api.mvc._
import play.api.Logger
import play.api.cache.Cache
import play.api.mvc.Call
import play.api.libs.oauth.OAuth
import play.api.data._
import play.api.data.Forms._

import java.util.UUID
import reactivesecurity.core.util.OauthUserData
import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext, future}
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

  def onUnauthorized(implicit request: RequestHeader): SimpleResult

  def onLoginSucceeded(provider: String)(implicit request: RequestHeader): SimpleResult

  def onLogoutSucceeded(implicit request: RequestHeader): SimpleResult

  def onStillLoggedIn(implicit request: RequestHeader): SimpleResult

  def getLoginPage(implicit request: RequestHeader): SimpleResult

  def onNewOauthUser(data: reactivesecurity.core.util.OauthUserData)(implicit request: RequestHeader): SimpleResult

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

  def associateProvider(provider: String) = Authenticated { implicit request =>
    import play.api.Play.current
    val callback = RoutesHelper.associateProviderCallback(provider)
    val linkedin = oauth1providers("linkedin")
    Cache.set("temp",request.user.id)
    linkedin.maybeService.map { oauth =>
      oauth1RetrieveRequestToken(oauth,callback.absoluteURL())
    }.getOrElse {
      ServiceUnavailable
    }
  }

  def associateProviderCallback(provider: String)(f: RequestHeader => AssociateProviderResult[USER] => Future[SimpleResult])= Action.async { implicit request =>
    import play.api.Play.current
    val uid = Cache.get("temp")
    val linkedin = oauth1providers("linkedin")
    linkedin.authenticate(request).flatMap {
      case Success(user) => f(request)(AssociateAlreadyInUse[USER](user))
      case Failure(RequiresNewOauthUser(oauth)) => {
        val zzz = uid.get.asInstanceOf[USER#ID]
        f(request)(AssociateSuccessful[USER](zzz,oauth))
      }
      case Failure(fail) => { f(request)(AssociateFailed("")) }
    }
  }

  def authenticate(provider: String) = handleAuth(provider)
  def authenticateByPost(provider: String) = handleAuth(provider)

  def oauth1RetrieveRequestToken[A](service: OAuth, callbackUrl: String)(implicit request: Request[A]): SimpleResult = {
    import play.api.Play.current
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] callback url = " + callbackUrl)
    }
    service.retrieveRequestToken(callbackUrl) match {
      case Right(requestToken) =>  {
        val cacheKey = UUID.randomUUID().toString
        val redirect = Redirect(service.redirectUrl(requestToken.token)).withSession(request.session +
          (OAuth1Provider.CacheKey -> cacheKey))
        Cache.set(cacheKey, requestToken, 600) // set it for 10 minutes, plenty of time to log in
        redirect
      }
      case Left(e) => {
        Logger.error("[reactivesecurity] error retrieving request token", e)
        Ok("Todo -- Handle error case when retrieving request token")
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
    Redirect( url ).withSession(request.session + ("sid", sessionId))
  }

  def handleOAuth1[A](p: OAuth1Provider[USER])(implicit request: Request[A]): Future[SimpleResult] = {
    val callback = RoutesHelper.authenticate(p.providerId).absoluteURL()
    handleGenericAuth(p) {
      case OauthNoVerifier => p.maybeService.map { service =>
        oauth1RetrieveRequestToken(service,callback)
      }.getOrElse {
        if ( Logger.isDebugEnabled ) {
          Logger.debug(s"[reactivesecurity] Error using Oauth Service: ${p.providerId}")
        }
        onUnauthorized(request)
      }
      case _ => onUnauthorized(request)
    }
  }

  def handleOAuth2[A](p: OAuth2Provider[USER])(implicit request: Request[A]): Future[SimpleResult] = {
    val callback = RoutesHelper.authenticate(p.providerId).absoluteURL()
    handleGenericAuth(p) { fail => fail match {
      case OAuth2NoAccessCode => p.maybeSettings.map {
        settings => oauth2RetrieveAccessCode(settings,callback)
      }.getOrElse(onUnauthorized(request))
      case _ => onUnauthorized(request)
    }}
  }

  def handleGenericAuth[A](p: Provider[USER])(onFail: AuthenticationFailure => SimpleResult)(implicit request: Request[A]): Future[SimpleResult] = {
    p.authenticate(request).flatMap {
      case Success(user) => completeAuthentication(user)(onLoginSucceeded(p.providerId))
      case Failure(RequiresNewOauthUser(oauth)) => { println("OMG") ; Future(onNewOauthUser(oauth)) }
      case Failure(fail) => Future(onFail(fail))
    }
  }

  lazy val userpass = new UserPasswordFormProvider(userService,passService)

  lazy val oauth1providers = Map[String, OAuth1Provider[USER]](
    "linkedin" -> new LinkedInProvider[USER](userService)
  )

  lazy val oauth2providers = Map[String,OAuth2Provider[USER]](
    "google" -> new GoogleProvider[USER](userService)
  )

  private def handleAuth(provider: String) = Action.async { implicit request =>
    Logger.debug("[reactivesecurity] Authorizing with provider: "+provider)
    if(provider == "userpass") {
      handleGenericAuth(userpass){ fail => onUnauthorized(request) }
    }
    else {
      oauth1providers.get(provider).map { oauth1 =>
        handleOAuth1(oauth1)
      }.getOrElse { oauth2providers.get(provider).map { oauth2 =>
        handleOAuth2(oauth2)
      }.getOrElse(future { onUnauthorized(request) }) }
    }
  }

  def completeAuthentication(user: USER)(onSuccess: SimpleResult)(implicit request: RequestHeader): Future[SimpleResult] = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] user logged in : [" + user + "]")
    }
    val secured = play.api.Play.current.configuration.getString("https.port").isDefined
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