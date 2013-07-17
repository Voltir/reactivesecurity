package reactivesecurity.defaults

import scala.collection._
import scala.concurrent._
import scalaz._

import reactivesecurity.core.User.{UsingID, UserService}
import reactivesecurity.core.std.{UserServiceFailure, IdentityNotFound}

class InMemoryUserService[ID,USER <: UsingID[ID]] extends UserService[ID,USER,UserServiceFailure] {

  private val users: mutable.Map[ID,USER] = mutable.Map()

  override def find(id: ID)(implicit ec: ExecutionContext) = future {
    users.get(id).map(Success(_)).getOrElse(Failure(IdentityNotFound(id)))
  }

  //TODO Make future?
  override def save(user: USER): Unit = {
    users += user.id -> user
  }
}