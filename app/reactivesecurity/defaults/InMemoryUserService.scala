package reactivesecurity.defaults

import scala.collection._
import scala.concurrent._
import scalaz._

import reactivesecurity.core.User.{UsingID, UserService}
import reactivesecurity.core.std.{UserServiceFailure, IdentityNotFound}

class InMemoryUserService[USER <: UsingID] extends UserService[USER] {

  private var users: mutable.Map[USER#ID,USER] = mutable.Map()

  override def find(id: USER#ID)(implicit ec: ExecutionContext) = future {
    println("Finding: "+ id)
    println(users)
    users.get(id).map(Success(_)).getOrElse(Failure(IdentityNotFound(id)))
  }

  //TODO Make future?
  override def save(user: USER): Unit = {
    println("Saving: "+user)
    users += (user.id -> user)
    println(users)
  }
}