package reactivesecurity.core.providers

import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.std.{IdentityNotFound, UserServiceFailure, AuthenticationServiceFailure, AuthenticationFailure}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import core.Credentials.PasswordHashValidator

case class IdPass(id: String, password: String)

class UserPasswordProvider[USER <: UsingID](users: UserService[USER], passService: PasswordService[USER]) extends AuthenticationService[IdPass,USER,AuthenticationFailure] {

  private val validator = new PasswordHashValidator[USER] { override val passwordService = passService }

  override def authenticate(credentials: IdPass): Future[Validation[AuthenticationFailure,USER]] = {
    val id = users.strAsId(credentials.id)
    users.find(id).map { _.fold {
      val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(IdentityNotFound(id)))
      fail
    } { user =>validator.validate(user,credentials) } }
  }
}