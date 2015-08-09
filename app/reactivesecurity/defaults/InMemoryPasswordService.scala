package reactivesecurity.defaults

import reactivesecurity.core.Password._
import reactivesecurity.core.service.HasID
import scala.concurrent.{ExecutionContext, Future}

class InMemoryPasswordService[User <: HasID] extends PasswordService[User] {
  private val passwords: scala.collection.concurrent.Map[User#ID,PasswordInfo] =
    new scala.collection.concurrent.TrieMap[User#ID,PasswordInfo]()

  override val hasher: PasswordHasher = BCryptHasher

  override def find(id: User#ID)(implicit ec: ExecutionContext): Future[Option[PasswordInfo]] = {
    val password = passwords.get(id)
    Future(password)
  }
}
