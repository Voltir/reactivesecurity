package core

//import core.Failures.{AuthenticationServiceFailure, AuthenticationFailure}
//import core.User.UsingID
//import play.api.mvc._
//import play.api.mvc.Cookie
//import org.joda.time.DateTime
//import scala.concurrent.{Future, ExecutionContext}
//import scalaz.{Success, Failure, Validation}
//
//
//
//case class AuthenticatorToken[USER <: UsingID](
//  id: Array[Byte],
//  uid: USER#ID,
//  creation: DateTime,
//  lastUsed: DateTime,
//  expiration: DateTime) {
//}
//
//trait AuthenticatorTokenStore[USER <: UsingID] {
//  def touch(id: Array[Byte]): Future[Option[AuthenticatorToken[USER]]]
//
//  def save(token: AuthenticatorToken[USER]): Future[Validation[Error, Unit]]
//
//  def delete(token: AuthenticatorToken[USER]): Future[Validation[Error, Unit]]
//}
//
//trait AuthenticationCookies[USER <: UsingID] {
//  def apply(token: AuthenticatorToken[USER], secure: Boolean): Cookie
//
//  def decodeId(id: String): Array[Byte]
//
//  def generateId: Array[Byte]
//}
//
//abstract class Authenticator[USER <: UsingID] {
//
//  val cookies: AuthenticationCookies[USER]
//
//  val store: AuthenticatorTokenStore[USER]
//
//  def touch(request: RequestHeader)(implicit ec: ExecutionContext): Future[Option[AuthenticatorToken[USER]]] = {
//    request.cookies.get(CookieParameters.cookieName).map { cookie =>
//      store.touch(cookies.decodeId(cookie.value))
//    }.getOrElse {
//      Future(None)
//    }
//  }
//
//  def create(uid: USER#ID, expireIn: org.joda.time.Duration)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure, AuthenticatorToken[USER]]] = {
//    val id = cookies.generateId
//    val now = DateTime.now()
//    val expirationDate = now.plus(expireIn)
//    val token = AuthenticatorToken(id, uid, now, now, expirationDate)
//    store.save(token).map {
//      case Failure(e) => Failure(AuthenticationServiceFailure(e))
//      case _ => Success(token)
//    }
//  }
//
//  def delete(token: AuthenticatorToken[USER])(implicit ec: ExecutionContext): Future[Validation[Error, Unit]] = store.delete(token)
//}
//
//object CookieParameters {
//  import play.api.Play.current
//
//  val defaultTimeout = 12*60
//  lazy val CookieDomainKey = "reactivesecurity.cookie.domain"
//  lazy val cookieName = "id"
//  lazy val cookiePath = "/"
//  lazy val cookieDomain = play.api.Play.application.configuration.getString(CookieDomainKey)
//}
