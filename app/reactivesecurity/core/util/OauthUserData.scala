package reactivesecurity.core.util

import reactivesecurity.core.Failures.{RequiresNewOauthUser,AuthenticationFailure}
import reactivesecurity.core.service.UserService
import scala.concurrent._
import play.api.libs.oauth.RequestToken

sealed trait OauthAccessToken
case class Oauth1(token: RequestToken) extends OauthAccessToken
case class Oauth2(token: String) extends OauthAccessToken

case class OauthUserData(
  provider: String,
  identifier: String,
  first: String,
  last: String,
  full: String,
  email: String,
  accessToken: OauthAccessToken)

object OauthAuthenticationHelper {
  def finishAuthenticate[User](provider: String, userService: UserService[User], oauth: OauthUserData)
                              (implicit ec: ExecutionContext): Future[Either[AuthenticationFailure,User]] = {
    userService.findByProvider(provider,oauth.identifier).map { user =>
      if(user.isDefined)  Right(user.get)
      else Left(RequiresNewOauthUser(oauth))
    }
  }
}