package reactivesecurity.defaults

import scala.concurrent._

import reactivesecurity.core.User.{UsingID, UserService}

abstract class InMemoryUserService[USER <: UsingID] extends UserService[USER] { /*this: StringAsID[USER] =>*/

  private var users: scala.collection.concurrent.Map[USER#ID,USER] =
    new scala.collection.concurrent.TrieMap[USER#ID,USER]()

  override def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[USER]] = {
    val result = users.get(id)
    future { result }
  }

  override def findMany(ids: List[USER#ID])(implicit ec: ExecutionContext): Future[List[USER]] = {
    val result = ids.flatMap(id => users.get(id))
    future { result }
  }

  override def save(user: USER)(implicit ec: ExecutionContext): Future[Boolean] = {
    users += (user.id -> user)
    future { true }
  }
}