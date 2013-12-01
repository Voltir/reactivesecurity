package reactivesecurity.defaults

import java.security.SecureRandom
import scalaz._

import play.api.libs.Codecs
import play.api.cache.Cache
import play.api.Play.current

import reactivesecurity.core.{AuthenticatorToken, AuthenticatorStore, CookieIdGenerator, Authenticator}

object DefaultCookieIdGenerator extends CookieIdGenerator {
  val random = new SecureRandom()
  val IdSizeInBytes = 128

  def generate: String = {
    var randomValue = new Array[Byte](IdSizeInBytes)
    random.nextBytes(randomValue)
    Codecs.toHexString(randomValue)
  }
}

object LocalCacheAuthenticationStore extends AuthenticatorStore {
  override def save(token: AuthenticatorToken): Validation[Error, Unit] = {
    Cache.set(token.id,token)
    Success(Unit)
  }

  override def find(id: String): Option[AuthenticatorToken] = Cache.getAs[AuthenticatorToken](id)

  override def delete(token: AuthenticatorToken) = {
    Cache.remove(token.id)
    Success(Unit)
  }
}

object LocalCacheAuthenticator extends Authenticator {
  override val cookieIdGen = DefaultCookieIdGenerator
  override val store =  LocalCacheAuthenticationStore
}
