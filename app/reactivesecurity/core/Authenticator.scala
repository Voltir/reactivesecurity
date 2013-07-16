package reactivesecurity.core

import scalaz.{Failure, Success, Validation}

import reactivesecurity.core.std.{UserServiceFailure, AuthenticationServiceFailure, AuthenticationFailure}
import org.joda.time.DateTime
import play.api.mvc.{AnyContent, Request, RequestHeader, Cookie}
import play.Play

case class CookieNotFound() extends UserServiceFailure

trait CookieIdGenerator {
  def generate(): String
}

trait AuthenticatorStore {
  def save(authenticator: AuthenticatorToken): Validation[Error, Unit]
  def find(id: String): Option[AuthenticatorToken]
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

  def find(request: Request[AnyContent]):Option[AuthenticatorToken] =
    request.cookies.get(CookieParameters.cookieName).fold(None: Option[AuthenticatorToken])(c => store.find(c.value))

  def create(userIdString: String): Validation[AuthenticationFailure, AuthenticatorToken] = {
    val id = cookieIdGen.generate()
    val now = DateTime.now()
    val expirationDate = now.plusMinutes(todo_absoluteTimeout)
    val token = AuthenticatorToken(id, userIdString, now, now, expirationDate)
    store.save(token).fold( e => Failure(AuthenticationServiceFailure(e)), _ => Success(token) )
  }
}

object CookieParameters {
  val CookieDomainKey = "reactivesecurity.cookie.domain"
  val cookieName = "id"
  val cookiePath = "/"
  val cookieDomain = Play.application.configuration.getString(CookieDomainKey)
}
