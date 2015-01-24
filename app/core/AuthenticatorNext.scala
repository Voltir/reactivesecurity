package core

import core.Password.PasswordService
import core.User.{UsingID, UserService}
import core.providers._
import core.Failures._
import core.util.OauthUserData

import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

trait AuthenticatorNext[In,Failure,Out,User <: UsingID] {

  val userService: UserService[User]
  val passService: PasswordService[User]
  val authService: AuthenticatorService[User,Failure]
  val oauth2Service: OAuth2Service[In,Out]

  def emailPassExtract: In => EmailPass

  def onNewOauthUser(oauth: OauthUserData): Future[Out]
  def onAuthenticationFailure(failure: AuthenticationFailure): Future[Out]
  def onUnknownProvider: Future[Out]

  lazy val providers: Map[String,Provider2[In,User]] = Map(
    "emailpass" -> EmailPasswordProvider2(userService,passService,emailPassExtract),
    //"linkedin" -> LinkedInProvider[User](userService),
    //"twitter" -> TwitterProvider[User](userService),
    "google" -> GoogleProvider2[In,Out,User](oauth2Service,userService)
  )

  def authenticate(in: In, provider: String)(success: User => AuthenticationToken => Future[Out]): Future[Out] = providers.get(provider).map {
    case pwd: EmailPasswordProvider2[In,User] => handleGenericAuth(pwd)(in)(onAuthenticationFailure)(success)
    //case oauth1: OAuth1Provider[User] => handleOAuth1(pwd)(in)(success)
    case oauth2: OAuth2Provider[In,Out,User] => handleOAuth2(oauth2)(in)(success)
    case _ => onUnknownProvider
  }.getOrElse(onUnknownProvider)

  private def handleGenericAuth
      (p: Provider2[In,User])
      (in: In)
      (onFail: AuthenticationFailure => Future[Out])
      (success: User => AuthenticationToken => Future[Out]): Future[Out] = {
    p.authenticate(in).flatMap {
      case Success(user) => authService.create(user.id).flatMap(token => success(user)(token))
      case Failure(RequiresNewOauthUser(oauth)) => onNewOauthUser(oauth)
      case Failure(f) => onFail(f)
    }
  }

  private def handleOAuth2
      (p: OAuth2Provider[In,Out,User])
      (in: In)
      (success: User => AuthenticationToken => Future[Out]): Future[Out] = {
    handleGenericAuth(p)(in) {
      case OAuth2NoAccessCode => oauth2Service.retrieveAccessCode(in)(p.providerId,p.maybeSettings.get)
      case other => onAuthenticationFailure(other)
    }(success)
  }
}
