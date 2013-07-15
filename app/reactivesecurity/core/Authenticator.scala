package reactivesecurity.core

import reactivesecurity.core.User.UserWithIdentity

import scalaz.{Failure, Success, Validation}

import _root_.java.security.SecureRandom
import reactivesecurity.core.std.{AuthenticationServiceFailure, AuthenticationFailure}
import org.joda.time.DateTime
import play.api.mvc.Cookie
import play.Play

trait CookieIdGenerator {
  def generate(): String
}

trait AuthenticatorStore {
  def save(authenticator: AuthenticatorToken): Validation[Error, Unit]
  //def find(id: String): Validation[Error, Option[Authenticator]]
  //def delete(id: String): Validation[Error, Unit]
}

case class AuthenticatorToken(id: String, uid: String, creation: DateTime, lastUsed: DateTime, expiration: DateTime) {
  def toCookie: Cookie = {
    import CookieParameters._
    Cookie(
      cookieName,
      id,
      Some(60),
      cookiePath,
      None
      //secure = cookieSecure,
      //httpOnly =  cookieHttpOnly
  )

  }
}
abstract class Authenticator {
  val cookieIdGen: CookieIdGenerator
  val store: AuthenticatorStore

  val todo_absoluteTimeout = 5

  def create(userIdString: String): Validation[AuthenticationFailure, AuthenticatorToken] = {
    val id = cookieIdGen.generate()
    val now = DateTime.now()
    val expirationDate = now.plusMinutes(todo_absoluteTimeout)
    val authenticator = AuthenticatorToken(id, userIdString, now, now, expirationDate)
    store.save(authenticator).fold( e => Failure(AuthenticationServiceFailure(e)), _ => Success(authenticator) )
  }
}

object CookieParameters {
  val CookieDomainKey = "reactivesecurity.cookie.domain"

  val cookieName = "id"
  val cookiePath = "/"
  val cookieDomain = Play.application.configuration.getString(CookieDomainKey)
}
