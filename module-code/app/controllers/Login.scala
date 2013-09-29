package reactivesecurity.controllers

import reactivesecurity.core._
import reactivesecurity.core.providers._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.std.{OauthNoVerifier, AuthenticationFailure}
import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.Password.PasswordService

import play.api.mvc._
import play.api.Logger
import play.api.cache.Cache
import play.api.mvc.Call
import play.api.libs.oauth.OAuth

import java.util.UUID
import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext, future}
import ExecutionContext.Implicits.global


abstract class Login[USER <: UsingID] extends Controller {
  val userService: UserService[USER]
  val passService: PasswordService[USER]

  val userPasswordProvider: UserPasswordProvider[USER]   //todo, delete this

  val authenticator: Authenticator

  def onUnauthorized(request: RequestHeader): Result
  def onLoginSucceeded(request: RequestHeader): Result
  def onLogoutSucceeded(request: RequestHeader): Result
  def getLoginPage(request: RequestHeader): Result

  def userFromOauthData(todo: ThisDoesNotBelongHere): USER

  def login = Action { implicit request =>
    withRefererAsOriginalUrl(getLoginPage(request))
  }

  def logout = Action { implicit request =>
    Ok("Todo")
  }

  def authenticate(provider: String) = handleAuth(provider)
  def authenticateByPost(provider: String) = handleAuth(provider)

  def oauthRetrieveRequestToken[A](service: OAuth, callbackUrl: String)(implicit request: Request[A]): Result = {
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

  def handleOauth[A](p: OAuth1Provider[USER])(implicit request: Request[A]): Future[Result] = {
    //??????????????
    lazy val conf = play.api.Play.current.configuration
    lazy val pc = play.Play.application().classloader().loadClass("controllers.ReverseLogin")
    lazy val loginMethods = pc.newInstance().asInstanceOf[{
      def authenticate(p: String): Call
      def login: Call
      def notAuthorized: Call
    }]
   //val callbackUrl = RoutesHelper.authenticate(id).absoluteURL(IdentityProvider.sslEnabled)
    val callback = loginMethods.authenticate(p.id).absoluteURL()
    //??????????????
    handleGenericAuth(p) { fail => fail match {
      case _: OauthNoVerifier => p.maybeService.map {
        service => oauthRetrieveRequestToken(service,callback)
        }.getOrElse(onUnauthorized(request))
      case _ => onUnauthorized(request)
    }}
  }

  def handleGenericAuth[A](p: AuthenticationService[Request[A],USER,AuthenticationFailure])(onFail: AuthenticationFailure => Result)(implicit request: Request[A]): Future[Result] = {
    p.authenticate(request).map { _.fold(
      fail => onFail(fail),
      (user: USER) => {
        //super hacks... really really want to fix this later...
        userService.save(user)
        completeAuthentication(user,session)
      }
    )}
  }

  val userpass = new UserPasswordFormProvider(userService,passService)

  val oauth1providers = Map[String, OAuth1Provider[USER]](
    "linkedin" -> new LinkedInProviderMK2[USER](userFromOauthData)
  )

  private def handleAuth(provider: String) = Action { implicit request => Async {
    Logger.debug("[reactivesecurity] Authorizing with provider: "+provider)

    if(provider == "userpass") handleGenericAuth(userpass){ fail => onUnauthorized(request) }

    oauth1providers.get(provider) match {
      case Some(p) => handleOauth(p)
      case None => future { onUnauthorized(request) }
    }
  }}

  def completeAuthentication(user: USER, session: Session)(implicit request: RequestHeader): Result = {
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[reactivesecurity] user logged in : [" + user + "]")
    }
    //TODO val withSession = Events.fire(new LoginEvent(user)).getOrElse(session)
    Logger.debug("[reactivesecurity] completeAuthentication -- This should work... "+user.id)
    authenticator.create(user.id.toString) match {
      case Failure(_) => onUnauthorized(request)
      case Success(token) => {
        println("Cookie: "+token.toCookie)
        onLoginSucceeded(request).withCookies(token.toCookie)
      }
    }
  }
}