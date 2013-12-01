package reactivesecurity.core.providers

import reactivesecurity.controllers.LoginForm
import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.std._
import reactivesecurity.core.std.AuthenticationServiceFailure
import reactivesecurity.core.std.IdentityNotFound
import scalaz.{Success, Failure, Validation}
import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import core.Credentials.PasswordHashValidator
import play.api.mvc.{AnyContent, Request}
import concurrent.future
import scalaz.Success
import scalaz.Failure
import play.api.Logger

case class IdPass(id: String, password: String)

//todo: I believe I can remove this class (use form provider defined below
class UserPasswordProvider[USER <: UsingID](users: UserService[USER], passService: PasswordService[USER]) extends AuthenticationService[IdPass,USER,AuthenticationFailure] {
  private val validator = new PasswordHashValidator[USER] { override val passwordService = passService }

  override def authenticate(credentials: IdPass): Future[Validation[AuthenticationFailure,USER]] = {
    val id = users.idFromEmail(credentials.id)
    users.find(id).flatMap { _.fold {
      val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(IdentityNotFound(id)))
      future { fail }
    } { user =>validator.validate(user,credentials) } }
  }
}


class UserPasswordFormProvider[USER <: UsingID](users: UserService[USER], passService: PasswordService[USER]) extends AuthenticationService[Request[AnyContent],USER,AuthenticationFailure] {
  private val validator = new PasswordHashValidator[USER] { override val passwordService = passService }

  override def authenticate(credentials: Request[AnyContent]): Future[Validation[AuthenticationFailure,USER]] = {
    val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(OauthFailure("Oauth Failed to Authenticate")))
    LoginForm.loginForm.bindFromRequest()(credentials).fold(
      errors => future { fail },
      { case (id: String, password: String) =>
        val uid = users.idFromEmail(id)
        users.find(uid).flatMap { _.fold {
          val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(IdentityNotFound(uid)))
          Logger.debug("[reactivesecurity] Userpass Authentication Failed -- Id Not Found: "+uid)
          future { fail }
          } { user => validator.validate(user,IdPass(id,password)) }
        }
      }
    )
  }
}