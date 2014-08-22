package reactivesecurity.defaults

import scala.concurrent._

import reactivesecurity.core.User.{UsingID, UserService}

abstract class InMemoryUserService[USER <: UsingID] extends UserService[USER] {

  protected var users: scala.collection.concurrent.Map[USER#ID,USER] =
    new scala.collection.concurrent.TrieMap[USER#ID,USER]()

  override def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[USER]] = {
    val result = users.get(id)
    Future(result)
  }
}