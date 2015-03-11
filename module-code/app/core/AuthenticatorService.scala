package reactivesecurity.core

//import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID

//import scala.concurrent.Future
//import scalaz.Validation

//import reactivesecurity.core.User.UsingID
//
//import scala.concurrent.Future
//import scalaz.Validation
//
//case class AuthenticationToken(uuid: String)
//
//trait AuthenticatorService[User <: UsingID, Failure] {
//  def create(user: User): Future[AuthenticationToken]
//  def get(token: AuthenticationToken): Future[Validation[Failure,User]]
//  def delete(uid: User#ID): Future[Unit]
//}