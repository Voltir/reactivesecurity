package reactivesecurity.core

import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.User.{UsingID, UserService}
import reactivesecurity.core.providers._
import reactivesecurity.core.Failures._
import reactivesecurity.core.util.OauthUserData

import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

trait AuthenticatorNext[In,Failure,Out,User <: UsingID] {

  val userService: UserService[User]
  val passService: PasswordService[User]
  val oauth2Service: OAuth2Service[In,Out]

  def emailPassExtract: In => EmailPass

  def onNewOauthUser(oauth: OauthUserData): Future[Out]
  def onAuthenticationFailure(failure: AuthenticationFailure): Future[Out]
  def onUnknownProvider: Future[Out]

  lazy val providers: Map[String,Provider2[In,User]] = Map(
    "emailpass" -> new EmailPasswordProvider2(userService,passService,emailPassExtract),
    //"linkedin" -> LinkedInProvider[User](userService),
    //"twitter" -> TwitterProvider[User](userService),
    "google" -> new GoogleProvider2[In,Out,User](oauth2Service,userService)
  )

  //def authenticate(in: In, provider: String)(success: User => AuthenticationToken => Future[Out]): Future[Out] = providers.get(provider).map {
  def authenticate(in: In, provider: String)(success: User => Future[Out]): Future[Out] = providers.get(provider).map {
    case pwd: EmailPasswordProvider2[In,User] => handleGenericAuth(pwd)(in)(onAuthenticationFailure)(success)
    //case oauth1: OAuth1Provider[User] => handleOAuth1(pwd)(in)(success)
    case oauth2: OAuth2Provider[In,Out,User] => handleOAuth2(oauth2)(in)(success)
    case _ => onUnknownProvider
  }.getOrElse(onUnknownProvider)

  private def handleGenericAuth
      (p: Provider2[In,User])
      (in: In)
      (onFail: AuthenticationFailure => Future[Out])
      (success: User => Future[Out]): Future[Out] = {
    p.authenticate(in).flatMap {
      //case Success(user) => authService.create(user).flatMap(token => success(user)(token))
      case Success(user) => success(user)
      case Failure(RequiresNewOauthUser(oauth)) => onNewOauthUser(oauth)
      case Failure(f) => onFail(f)
    }
  }

  private def handleOAuth2
      (p: OAuth2Provider[In,Out,User])
      (in: In)
      (success: User => Future[Out]): Future[Out] = {
    handleGenericAuth(p)(in) {
      case OAuth2NoAccessCode => oauth2Service.retrieveAccessCode(in)(p.providerId,p.maybeSettings.get)
      case other => onAuthenticationFailure(other)
    }(success)
  }
}
