package defaults

import reactivesecurity.core.Password._
import reactivesecurity.core.User.UsingID
import reactivesecurity.defaults.BCryptHasher
import scala.collection.mutable

class InMemoryPasswordService[USER <: UsingID] extends PasswordService[USER] {

  private val passwords: mutable.Map[USER#ID,PasswordInfo] = mutable.Map()

  override val hasher: PasswordHasher = BCryptHasher

  override def find(id: USER#ID): Option[PasswordInfo] = passwords.get(id)

  override def save(id: USER#ID, pass: PasswordInfo): Unit = passwords += (id -> pass)
}
