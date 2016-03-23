package reactivesecurity.core

import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.providers._
import reactivesecurity.core.Failures._
import reactivesecurity.core.service._
import reactivesecurity.core.util.OauthUserData

import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

trait Authenticator[In,Failure,Out,User] {

  def userService: UserService[User]
  val passService: PasswordService[Identifiable[User]#ID]
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

  def authenticate
      (in: In, provider: String)
      (success: User => Future[Out])
      (implicit id: Identifiable[User]): Future[Out] = providers.get(provider).map {
    case pwd: EmailPasswordProvider2[In,User] => handleGenericAuth(pwd)(in)(onAuthenticationFailure)(success)
    //case oauth1: OAuth1Provider[User] => handleOAuth1(pwd)(in)(success)
    case oauth2: OAuth2Provider[In,Out,User] => handleOAuth2(oauth2)(in)(success)
    case _ => onUnknownProvider
  }.getOrElse(onUnknownProvider)

  private def handleGenericAuth
      (p: Provider2[In,User])
      (in: In)
      (onFail: AuthenticationFailure => Future[Out])
      (success: User => Future[Out])
      (implicit id: Identifiable[User]): Future[Out] = {
    p.authenticate(in).flatMap {
      case Right(user) => success(user)
      case Left(RequiresNewOauthUser(oauth)) => onNewOauthUser(oauth)
      case Left(f) => onFail(f)
    }
  }

  private def handleOAuth2
      (p: OAuth2Provider[In,Out,User])
      (in: In)
      (success: User => Future[Out])
      (implicit id: Identifiable[User]): Future[Out] = {
    handleGenericAuth(p)(in) {
      case OAuth2NoAccessCode => oauth2Service.retrieveAccessCode(in)(p.providerId,p.maybeSettings.get)
      case other => onAuthenticationFailure(other)
    }(success)
  }
}
