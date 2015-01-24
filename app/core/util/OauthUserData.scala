package core.util

import core.Failures.{RequiresNewOauthUser,AuthenticationFailure}
import core.User.{UserService, UsingID}
import scala.concurrent._
import scalaz.{Success, Failure, Validation}
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
  def finishAuthenticate[USER <: UsingID](provider: String, userService: UserService[USER], oauth: OauthUserData)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]] = {
    userService.findByProvider(provider,oauth.identifier).map { user =>
      if(user.isDefined)  {
        //userService.oauthUpdateAccessToken(user.get.id,provider,oauth.accessToken)
        Success(user.get)
      }
      else Failure(RequiresNewOauthUser(oauth))
    }
  }
}