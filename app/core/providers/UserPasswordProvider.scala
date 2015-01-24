package core.providers

import core.Password.PasswordService
import core.Failures._
import core.Provider2
import core.Credentials.PasswordHashValidator2
import core.User.{UserService, UsingID}
import scalaz.{Failure, Validation}
import scala.concurrent.{ExecutionContext, Future}

case class EmailPass(email: String, password: String)

case class EmailPasswordProvider2[In, User <: UsingID](
    users: UserService[User],
    passService: PasswordService[User],
    extract: In => EmailPass
) extends Provider2[In, User] {
  override val providerId = "emailpass"

  private val validator = new PasswordHashValidator2[User] { override val passwordService = passService }

  override def authenticate(input: In)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,User]] = {
    val credentials = extract(input)
    users.findByProvider(providerId,credentials.email).flatMap { _.fold {
      val fail: Validation[AuthenticationFailure,User] = Failure(AuthenticationServiceFailure(IdentityNotFound(credentials.email)))
      Future(fail)
    } {
      user => validator.validate(user,credentials)
    }}
  }
}