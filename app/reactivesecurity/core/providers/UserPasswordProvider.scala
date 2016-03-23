package reactivesecurity.core.providers

import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.Failures._
import reactivesecurity.core.Provider2
import reactivesecurity.core.Credentials.PasswordHashValidator
import reactivesecurity.core.service.{Identifiable, UserService}
import scala.concurrent.{ExecutionContext, Future}

case class EmailPass(email: String, password: String)

case class EmailPasswordProvider2[In, User](
    users: UserService[User],
    passService: PasswordService[Identifiable[User]#ID],
    extract: In => EmailPass
) extends Provider2[In, User] {

  override val providerId = "emailpass"

  private val validator = new PasswordHashValidator[User] { override val passwordService = passService }

  override def authenticate(input: In)(implicit ec: ExecutionContext, id: Identifiable[User]): Future[Either[AuthenticationFailure,User]] = {
    val credentials = extract(input)
    users.findByProvider(providerId,credentials.email).flatMap { _.fold {
      val fail: Either[AuthenticationFailure,User] = Left(AuthenticationServiceFailure(IdentityNotFound(credentials.email)))
      Future.successful(fail)
    } {
      user => validator.validate(user,credentials)
    }}
  }
}