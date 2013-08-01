package reactivesecurity.defaults

import scala.collection._
import scala.concurrent._
import scalaz._

import reactivesecurity.core.User.{StringAsID, UsingID, UserService}
import reactivesecurity.core.std.{UserServiceFailure, IdentityNotFound}

abstract class InMemoryUserService[USER <: UsingID] extends UserService[USER] { this: StringAsID[USER] =>

  private var users: mutable.Map[USER#ID,USER] = mutable.Map()

  override def find(id: USER#ID)(implicit ec: ExecutionContext) = future {
    users.get(id).map(Success(_)).getOrElse(Failure(IdentityNotFound(id)))
  }

  override def save(user: USER): Unit = {
    users += (user.id -> user)
  }

}