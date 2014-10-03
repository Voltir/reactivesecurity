package reactivesecurity.core

import reactivesecurity.core.util.{OauthAccessToken, OauthUserData}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.oauth.RequestToken

object User {

  trait UsingID {
    type ID
    val id: ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[USER <: UsingID] {
    def findByProvider(provider: String, identifier: String)(implicit ec: ExecutionContext): Future[Option[USER]]
    def oauthAccessToken(id: USER#ID, provider: String, token: OauthAccessToken): Unit
  }
}