package reactivesecurity.core.providers

import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.Failures._
import reactivesecurity.core.Provider2
import scalaz.{Failure, Validation}
import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{ExecutionContext, Future}
import core.Credentials.PasswordHashValidator2

case class EmailPass(email: String, password: String)

class EmailPasswordProvider2[In, User <: UsingID](
    users: UserService[User],
    passService: PasswordService[User],
    extract: In => EmailPass
) extends Provider2[In, User] {
  override val providerId = "emailpass"

  private val validator = new PasswordHashValidator2[User] { override val passwordService = passService }

  override def authenticate(input: In)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,User]] = {
    val credentials = extract(input)
    users.findByProvider(providerId,credentials.email).flatMap {
      case Some(user) => validator.validate(user,credentials)
      case _ => Future(Failure(IdentityNotFound[User](credentials.email)))
    }
  }
}