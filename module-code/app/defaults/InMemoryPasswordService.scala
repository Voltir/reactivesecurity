package defaults

import reactivesecurity.core.Password._
import reactivesecurity.core.User.UsingID
import reactivesecurity.defaults.BCryptHasher
import scala.concurrent.{ExecutionContext, Future, future}

class InMemoryPasswordService[USER <: UsingID] extends PasswordService[USER] {
  private val passwords: scala.collection.concurrent.Map[USER#ID,PasswordInfo] =
    new scala.collection.concurrent.TrieMap[USER#ID,PasswordInfo]()

  override val hasher: PasswordHasher = BCryptHasher

  override def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[PasswordInfo]] = {
    val password = passwords.get(id)
    future { password }
  }

  override def save(id: USER#ID, pass: PasswordInfo)(implicit ec: ExecutionContext): Future[Boolean] = {
    passwords += (id -> pass)
    future { true }
  }
}
