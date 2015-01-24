package reactivesecurity.core

import scala.concurrent.{ExecutionContext, Future}

object User {

  trait UsingID {
    type ID
    val id: ID
  }

  trait UserService[User <: UsingID] {
    def findByProvider(provider: String, identifier: String)(implicit ec: ExecutionContext): Future[Option[User]]
    //def oauthUpdateAccessToken(id: User#ID, provider: String, token: OauthAccessToken): Unit
  }
}