package reactivesecurity.defaults

import java.security.SecureRandom
import reactivesecurity.core.User.UsingID
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

class LocalCacheAuthenticationStore[USER <: UsingID] extends AuthenticatorStore[USER] {
  override def save(token: AuthenticatorToken[USER]): Validation[Error, Unit] = {
    Cache.set(token.id,token)
    Success(Unit)
  }

  override def find(id: String): Option[AuthenticatorToken[USER]] = {
    Cache.getAs[AuthenticatorToken[USER]](id)
  }

  override def delete(token: AuthenticatorToken[USER]) = {
    Cache.remove(token.id)
    Success(Unit)
  }
}

/*
class LocalCacheAuthenticator[USER <: UsingID] extends Authenticator {
  override val cookieIdGen = DefaultCookieIdGenerator
  override val store =  LocalCacheAuthenticationStore
}
*/
