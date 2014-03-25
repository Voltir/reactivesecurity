package reactivesecurity.core

import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import play.api.mvc._
import scala.concurrent.{Future, ExecutionContext}
import scalaz.Validation
import play.api.mvc.Cookie
import org.joda.time.DateTime


case class AuthenticatorToken[USER <: UsingID](
  id: Array[Byte],
  uid: USER#ID,
  creation: DateTime,
  lastUsed: DateTime,
  expiration: DateTime) {

  def toCookie(secure: Boolean): Cookie = ??? /*{
    import CookieParameters._
    Cookie(
      cookieName,
      id,
      Some(CookieParameters.absoluteTimeoutInSeconds),
      cookiePath,
      None,
      secure = secure,
      httpOnly =  true
    )
  }
  */
}

trait Authenticator[USER <: UsingID] {
  def touch(request: RequestHeader)(implicit ec: ExecutionContext): Future[Option[AuthenticatorToken[USER]]]
  def create(uid: USER#ID, expireIn: org.joda.time.Duration)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure, AuthenticatorToken[USER]]]
  def delete(token: AuthenticatorToken[USER])(implicit ec: ExecutionContext): Future[Validation[Error, Unit]]
}

/*
import reactivesecurity.core.User.UsingID
import scalaz.{Failure, Success, Validation}

import reactivesecurity.core.Failures._
import org.joda.time.DateTime
import play.api.mvc.{ RequestHeader, Cookie}
import scala.concurrent.{ExecutionContext, Future}

case class CookieNotFound() extends UserServiceFailure

trait CookieIdGenerator {
  def generate(): String
}

trait AuthenticatorStore[USER <: UsingID] {
  def save(token: AuthenticatorToken[USER]): Future[Validation[Error, Unit]]
  def find(id: String): Future[Option[AuthenticatorToken[USER]]]
  def delete(token: AuthenticatorToken[USER]): Future[Validation[Error, Unit]]
}

case class AuthenticatorToken[USER <: UsingID](
  id: String,
  uid: USER#ID,
  creation: DateTime,
  lastUsed: DateTime,
  expiration: DateTime) {

  def toCookie(secure: Boolean): Cookie = {
    import CookieParameters._
    Cookie(
      cookieName,
      id,
      Some(CookieParameters.absoluteTimeoutInSeconds),
      cookiePath,
      None,
      secure = secure,
      httpOnly =  true
    )
  }
}

abstract class Authenticator[USER <: UsingID] {
  val cookieIdGen: CookieIdGenerator
  val store: AuthenticatorStore[USER]

  def find(request: RequestHeader)(implicit ec: ExecutionContext): Future[Option[AuthenticatorToken[USER]]] = {
    request.cookies.get(CookieParameters.cookieName).map { cookie =>
      store.find(cookie.value)
    }.getOrElse {
      Future(None)
    }
  }

  def create(uid: USER#ID, expireIn: org.joda.time.Duration)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure, AuthenticatorToken[USER]]] = {
    val id = cookieIdGen.generate()
    val now = DateTime.now()
    val expirationDate = now.plus(expireIn)
    val token = AuthenticatorToken(id, uid, now, now, expirationDate)
    store.save(token).map {
      case Failure(e) => Failure(AuthenticationServiceFailure(e))
      case _ => Success(token)
    }
  }

  def delete(token: AuthenticatorToken[USER])(implicit ec: ExecutionContext): Future[Validation[Error, Unit]] = store.delete(token)
}
*/

object CookieParameters {
  import play.api.Play.current

  val defaultTimeout = 12*60
  lazy val CookieDomainKey = "reactivesecurity.cookie.domain"
  lazy val cookieName = "id"
  lazy val cookiePath = "/"
  lazy val cookieDomain = play.api.Play.application.configuration.getString(CookieDomainKey)
  lazy val absoluteTimeout = play.api.Play.application.configuration.getInt("reactivesecurity.cookie.timeout").getOrElse(defaultTimeout)
  lazy val absoluteTimeoutInSeconds = absoluteTimeout * 60
}
