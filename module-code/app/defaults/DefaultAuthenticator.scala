package reactivesecurity.defaults

/*
import java.security.SecureRandom
import reactivesecurity.core.User.UsingID
import scalaz._

import play.api.libs.Codecs
import play.api.cache.Cache
import play.api.Play.current

import reactivesecurity.core.{AuthenticatorToken, AuthenticatorStore, CookieIdGenerator, Authenticator}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object DefaultCookieIdGenerator extends CookieIdGenerator {
  val random = new SecureRandom()
  val IdSizeInBytes = 128

  def generate: String = {
    var randomValue = new Array[Byte](IdSizeInBytes)
    random.nextBytes(randomValue)
    Codecs.toHexString(randomValue)
  }
}

class LocalCacheAuthenticationStore[USER <: UsingID] extends AuthenticatorStore[USER] {
  override def save(token: AuthenticatorToken[USER]): Future[Validation[Error, Unit]] = {
    Cache.set(token.id,token)
    Future(Success(Unit))
  }

  override def find(id: String): Future[Option[AuthenticatorToken[USER]]] = {
    val result = Cache.getAs[AuthenticatorToken[USER]](id)
    Future(result)
  }

  override def delete(token: AuthenticatorToken[USER]) = {
    Cache.remove(token.id)
    Future(Success(Unit))
  }
}
*/