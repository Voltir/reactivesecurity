package reactivesecurity.core.providers

import reactivesecurity.controllers.LoginForm
import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.Failures._
import reactivesecurity.core.{Provider2, Provider}
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import core.Credentials.{PasswordHashValidator2, PasswordHashValidator}
import play.api.mvc.{AnyContent, Request}
import play.api.Logger

case class IdPass(id: String, password: String)


case class UserPassword[USER <: UsingID](users: UserService[USER], passService: PasswordService[USER]) extends Provider[USER] {

  override val providerId = "userpass"

  private val validator = new PasswordHashValidator[USER] { override val passwordService = passService }

  override def authenticate(credentials: Request[_]): Future[Validation[AuthenticationFailure,USER]] = {
    val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(OauthFailure("Oauth Failed to Authenticate")))
    LoginForm.loginForm.bindFromRequest()(credentials).fold(
      errors => Future(fail),
      { case (email: String, password: String) =>
        users.findByProvider(providerId,email).flatMap { _.fold {
          val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(IdentityNotFound(email)))
          Logger.debug("[reactivesecurity] Userpass Authentication Failed -- Email Not Found: "+email)
          Future(fail)
          } { user => validator.validate(user,IdPass(email,password)) }
        }
      }
    )
  }
}

case class EmailPass(email: String, password: String)

case class EmailPasswordProvider2[In, User <: UsingID](
    users: UserService[User],
    passService: PasswordService[User],
    extract: In => EmailPass
) extends Provider2[In, User] {
  override val providerId = "emailpass"

  private val validator = new PasswordHashValidator2[User] { override val passwordService = passService }

  override def authenticate(input: In): Future[Validation[AuthenticationFailure,User]] = {
    val credentials = extract(input)
    users.findByProvider(providerId,credentials.email).flatMap { _.fold {
      val fail: Validation[AuthenticationFailure,User] = Failure(AuthenticationServiceFailure(IdentityNotFound(credentials.email)))
      Logger.debug("[reactivesecurity] Userpass Authentication Failed -- Email Not Found: "+credentials.email)
      Future(fail)
    } {
      user => validator.validate(user,credentials)
    }}
  }
}