package defaults

import play.api.mvc.Cookie
import core.{CookieParameters, AuthenticatorToken, AuthenticationCookies}
import core.User.UsingID
import java.security.SecureRandom
import play.api.libs.Codecs._

/* Creates a cookie set to never expire, 64 bits UUID, assumes backend will delete
 * expired tokens, thus invalidating this cookie
 */
class DefaultAuthenticationCookies[USER <: UsingID] extends AuthenticationCookies[USER] {
  override def apply(token: AuthenticatorToken[USER], secure: Boolean): Cookie = {
    val longTime = 2 << 30 - 1
    Cookie(
      CookieParameters.cookieName,
      encodeId(token.id),
      Some(longTime),
      CookieParameters.cookiePath,
      None,
      secure = secure,
      httpOnly =  true
    )
  }

  override def generateId: Array[Byte] = DefaultCookieIdGenerator.generate

  def decodeId(encodedId: String): Array[Byte] = hexStringToByte(encodedId)

  private def encodeId(id: Array[Byte]): String = toHexString(id)
}

object DefaultCookieIdGenerator {
  val random = new SecureRandom()
  val IdSizeInBytes = 64

  def generate: Array[Byte] = {
    var randomValue = new Array[Byte](IdSizeInBytes)
    random.nextBytes(randomValue)
    randomValue
  }
}
