package reactivesecurity.core

import reactivesecurity.core.AuthenticatorService
import reactivesecurity.core.Authentication.AuthenticationProcess
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.User.{UsingID, UserService}
import reactivesecurity.core.providers._
import reactivesecurity.core.Failures._
import reactivesecurity.core.util.OauthUserData
import securesocial.core.providers.GoogleProvider

import scalaz.{Failure,Success}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

trait AuthenticatorNext[In,Failure,Out,User <: UsingID] /*extends AuthenticationProcess[In,Out,User]*/ {

  val userService: UserService[User]
  val passService: PasswordService[User]
  val authService: AuthenticatorService[User,Failure]

  def emailPassExtract: In => EmailPass

  def onNewOauthUser(oauth: OauthUserData): Future[Out]
  def onAuthenticationFailure(failure: AuthenticationFailure): Future[Out]
  def onUnknownProvider: Future[Out]

  lazy val providers: Map[String,Provider2[In,User]] = Map(
    "emailpass" -> EmailPasswordProvider2(userService,passService,emailPassExtract)
    //"linkedin" -> LinkedInProvider[User](userService),
    //"twitter" -> TwitterProvider[User](userService),
    //"google" -> GoogleProvider[User](userService)
  )


  def authenticate(in: In, provider: String)(success: User => AuthenticationToken => Future[Out]): Future[Out] = providers.get(provider).map {
    case pwd: EmailPasswordProvider2[In,User] => handleGenericAuth(pwd)(in)(success)
    //case oauth1: OAuth1Provider[User] => handleOAuth1(pwd)(in)(success)
    //case oauth2: OAuth2Provider[User] => handleOAuth2(pwd)(in)(success)
    case _ => onUnknownProvider
  }.getOrElse(onUnknownProvider)


  private def handleGenericAuth(p: Provider2[In,User])(in: In)(success: User => AuthenticationToken => Future[Out]): Future[Out] = {
    p.authenticate(in).flatMap {
      case Success(user) => {
        authService.create(user.id).flatMap(token => success(user)(token))
      }
      case Failure(RequiresNewOauthUser(oauth)) => onNewOauthUser(oauth)
      case Failure(f) => onAuthenticationFailure(f)
    }
  }
}
