package reactivesecurity.core.providers

import reactivesecurity.controllers.LoginForm
import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.Failures._
import reactivesecurity.core.Provider
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import core.Credentials.PasswordHashValidator
import play.api.mvc.{AnyContent, Request}
import concurrent.future
import play.api.Logger

case class IdPass(id: String, password: String)

case class UserPassword[USER <: UsingID](users: UserService[USER], passService: PasswordService[USER]) extends Provider[USER] {

  override val providerId = "userpass"

  private val validator = new PasswordHashValidator[USER] { override val passwordService = passService }

  override def authenticate(credentials: Request[_]): Future[Validation[AuthenticationFailure,USER]] = {
    val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(OauthFailure("Oauth Failed to Authenticate")))
    LoginForm.loginForm.bindFromRequest()(credentials).fold(
      errors => future { fail },
      { case (email: String, password: String) =>
        users.findByProvider(providerId,email).flatMap { _.fold {
          val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(IdentityNotFound(email)))
          Logger.debug("[reactivesecurity] Userpass Authentication Failed -- Email Not Found: "+email)
          future { fail }
          } { user => validator.validate(user,IdPass(email,password)) }
        }
      }
    )
  }
}