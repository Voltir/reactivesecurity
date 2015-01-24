package core

import core.util.{OauthAccessToken, OauthUserData}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.oauth.RequestToken

object User {

  trait UsingID {
    type ID
    val id: ID
  }

  trait UserService[User <: UsingID] {
    def findByProvider(provider: String, identifier: String)(implicit ec: ExecutionContext): Future[Option[User]]
    def oauthUpdateAccessToken(id: User#ID, provider: String, token: OauthAccessToken): Unit
  }
}