package reactivesecurity.core

import reactivesecurity.core.User.UsingID
import scalaz.{Failure, Success, Validation}

import reactivesecurity.core.Failures._
import org.joda.time.DateTime
import play.api.mvc.{AnyContent, Request, RequestHeader, Cookie}
import play.Play

//should be moved to Plat dir
case class CookieNotFound() extends UserServiceFailure

trait CookieIdGenerator {
  def generate(): String
}

trait AuthenticatorStore[USER <: UsingID] {
  def save(token: AuthenticatorToken[USER]): Validation[Error, Unit]
  def find(id: String): Option[AuthenticatorToken[USER]]
  def delete(token: AuthenticatorToken[USER]): Validation[Error, Unit]
}

case class AuthenticatorToken[USER <: UsingID](
  id: String,
  uid: USER#ID,
  creation: DateTime,
  lastUsed: DateTime,
  expiration: DateTime) {

  def toCookie: Cookie = {
    import CookieParameters._
    Cookie(
      cookieName,
      id,
      Some(3600*10), //TODO -- Use expiration?
      cookiePath,
      None,
      //secure = true,
      httpOnly =  true
    )
  }
}

abstract class Authenticator[USER <: UsingID] {
  val cookieIdGen: CookieIdGenerator
  val store: AuthenticatorStore[USER]

  val todo_absoluteTimeout = 5

  def find(request: RequestHeader):Option[AuthenticatorToken[USER]] = {
    request.cookies.get(CookieParameters.cookieName).fold(None: Option[AuthenticatorToken[USER]])(t => store.find(t.value))
  }

  def create(uid: USER#ID): Validation[AuthenticationFailure, AuthenticatorToken[USER]] = {
    val id = cookieIdGen.generate()
    val now = DateTime.now()
    val expirationDate = now.plusMinutes(todo_absoluteTimeout)
    val token = AuthenticatorToken(id, uid, now, now, expirationDate)
    store.save(token).fold( e => Failure(AuthenticationServiceFailure(e)), _ => Success(token) )
  }

  def delete(token: AuthenticatorToken[USER]) = store.delete(token)
}

object CookieParameters {
  val CookieDomainKey = "reactivesecurity.cookie.domain"
  val cookieName = "id"
  val cookiePath = "/"
  val cookieDomain = Play.application.configuration.getString(CookieDomainKey)
}
