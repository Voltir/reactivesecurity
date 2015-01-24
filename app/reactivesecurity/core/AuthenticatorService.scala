package reactivesecurity.core

import reactivesecurity.core.User.UsingID

import scala.concurrent.Future
import scalaz.Validation

case class AuthenticationToken(uuid: String)

trait AuthenticatorService[User <: UsingID, Failure] {
  def create(uid: User#ID): Future[AuthenticationToken]
  def get(token: AuthenticationToken): Future[Validation[Failure,User]]
  def delete(uid: User#ID): Future[Unit]
}
