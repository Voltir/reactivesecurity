package reactivesecurity.core

import reactivesecurity.core.User.UsingID

object Password {
  case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

  trait PasswordHasher {
    def id: String
    def hash(plainPassword: String): PasswordInfo
    def matches(passwordInfo: PasswordInfo, supplied: String): Boolean
  }

  trait PasswordService[USER <: UsingID] {
    val hasher: PasswordHasher
    def find(id: USER#ID): Option[PasswordInfo]
    def save(id: USER#ID, pass: PasswordInfo): Unit
  }
}