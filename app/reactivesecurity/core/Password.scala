package reactivesecurity.core
import reactivesecurity.core.service.Identifiable
import scala.concurrent.{Future, ExecutionContext}

object Password {
  case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

  trait PasswordHasher {
    def id: String
    def hash(plainPassword: String): PasswordInfo
    def matches(passwordInfo: PasswordInfo, supplied: String): Boolean
  }

  trait PasswordService[Identity] {
    def hasher: PasswordHasher
    def find(id: Identity)(implicit ec: ExecutionContext): Future[Option[PasswordInfo]]
  }
}