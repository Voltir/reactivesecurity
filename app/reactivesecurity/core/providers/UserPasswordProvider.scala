package reactivesecurity.core.providers

import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.Failures._
import reactivesecurity.core.Provider2
import reactivesecurity.core.Credentials.PasswordHashValidator2
import reactivesecurity.core.service.{HasID, UserService}
import scalaz.{Failure, Validation}
import scala.concurrent.{ExecutionContext, Future}

case class EmailPass(email: String, password: String)

case class EmailPasswordProvider2[In, User <: HasID](
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