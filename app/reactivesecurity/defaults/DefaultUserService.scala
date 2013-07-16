package reactivesecurity.defaults

import scala.collection._
import scala.concurrent._
import scalaz._

import reactivesecurity.core.User.{StringFromId, UserWithIdentity, UserService}
import reactivesecurity.core.std.{UserServiceFailure, IdentityNotFound}

class DefaultUserService[ID,USER <: UserWithIdentity[ID]] extends UserService[ID,USER,UserServiceFailure] {

  private val users: mutable.Map[ID,USER] = mutable.Map()

  override def find(id: ID)(implicit ec: ExecutionContext) = future {
    users.get(id).map(Success(_)).getOrElse(Failure(IdentityNotFound(id)))
  }
}