package reactivesecurity.defaults

import reactivesecurity.core.Password._
import reactivesecurity.core.service.Identifiable
import scala.concurrent.{ExecutionContext, Future}

class InMemoryPasswordService[User: Identifiable] extends PasswordService[Identifiable[User]#ID] {
  type ID = Identifiable[User]#ID
  private implicit val id = implicitly[Identifiable[User]]

  private val passwords: scala.collection.concurrent.Map[ID,PasswordInfo] =
    new scala.collection.concurrent.TrieMap[ID,PasswordInfo]()

  override val hasher: PasswordHasher = BCryptHasher

  override def find(id: ID)(implicit ec: ExecutionContext): Future[Option[PasswordInfo]] = {
    Future.successful(passwords.get(id))
  }
}
