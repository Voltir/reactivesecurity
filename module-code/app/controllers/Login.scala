package reactivesecurity.controllers

import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.std.OauthNoVerifier
import reactivesecurity.core.ThisDoesNotBelongHere
import reactivesecurity.core.ThisDoesNotBelongHere
import scala.concurrent.{Future, ExecutionContext, future}
import ExecutionContext.Implicits.global
import scalaz.{Failure,Success}

import play.api.mvc._
import play.api.{Play, Logger}

import reactivesecurity.core.providers._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.std.{OauthNoVerifier, AuthenticationFailure, AuthenticationServiceFailure}
import reactivesecurity.core.{ThisDoesNotBelongHere, Authenticator, OAuth1Provider}
import scalaz.Failure
import scala.Some
import play.api.mvc.Call
import scalaz.Success
import java.util.UUID
import play.api.mvc.Results._
import scalaz.Failure
import scala.Some
import play.api.mvc.Call
import scalaz.Success
import play.api.cache.Cache

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


  def iamgettingtired(callbackUrl: String, request: Request[AnyContent]): Result = {
    import play.api.Play.current
    val service = new LinkedInProviderMK2[USER](userFromOauthData).service //todo generalize this somehow
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
  //def handleOauth[A](p: LinkedInProvider[USER])(implicit request: Request[A]): Future[Either[Result, USER]] = {
  def handleOauth(p: AuthenticationService[Request[AnyContent],USER,AuthenticationFailure])(implicit request: Request[AnyContent]): Future[Result] = {
    //??????????????
    lazy val conf = play.api.Play.current.configuration
    lazy val pc = play.Play.application().classloader().loadClass("controllers.ReverseLogin")
    lazy val loginMethods = pc.newInstance().asInstanceOf[{
      def authenticate(p: String): Call
      def login: Call
      def notAuthorized: Call
    }]
    val callback = loginMethods.authenticate("linkedin").absoluteURL()
    //??????????????
    //p.authenticate(request).fold(
    /*
    p.rawrStab(callback)(userFromOauthData).map { _.fold(
      (result:Result) => Left(result),
      (u:USER) => {
        val saved = userService.save(u)
        Right(u)
      }
    )}
    */
    p.authenticate(request).map { _.fold(
      fail => fail match {
        case _: OauthNoVerifier => iamgettingtired(callback,request)
        case _ => onUnauthorized(request)
      },
      (user: USER) => {
        //super hacks... really really want to fix this later...
        userService.save(user)
        completeAuthentication(user,session)
      }
    )}

  }

  private def handleAuth(provider: String) = Action { implicit request => Async {
    Logger.debug("[reactivesecurity] Authorizing with provider: "+provider)
    val providers = Map[String, AuthenticationService[Request[AnyContent],USER,AuthenticationFailure]](
      "userpass" -> new UserPasswordFormProvider(userService,passService),
      "linkedin" -> new LinkedInProviderMK2[USER](userFromOauthData))
    providers.get(provider) match {
      case Some(p) => handleOauth(p)
      case None => future { onUnauthorized(request) }
    }
    /*
    val fuck = new LinkedInProvider[USER]()
    handleOauth(fuck).map { _.fold[Result](
      result => result,
      user => completeAuthentication(user,session)
    )}
    */
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
    /*
    LoginForms.loginForm.bindFromRequest().fold(
    errors => future { Ok("Errors") },
    { case (id: String, password: String) => {
      val credentials = IdPass(id,password)
      userPasswordProvider.authenticate(credentials).map { result =>
        result match {
          case Failure(AuthenticationServiceFailure(f)) => onUnauthorized(request)
          case Failure(wat) => onUnauthorized(request)
          case Success(user) => completeAuthentication(user,session)
        }
      }
    }}
    ) */
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