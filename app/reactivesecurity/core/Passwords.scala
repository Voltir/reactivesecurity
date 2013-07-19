package reactivesecurity.core

case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

trait PasswordHasher {
  def id: String
  def hash(plainPassword: String): PasswordInfo
  def matches(passwordInfo: PasswordInfo, supplied: String): Boolean
}