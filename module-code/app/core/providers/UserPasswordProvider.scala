package reactivesecurity.core.providers

import reactivesecurity.controllers.LoginForms
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


class UserPasswordFormProvider[USER <: UsingID](users: UserService[USER], passService: PasswordService[USER]) extends AuthenticationService[Request[AnyContent],USER,AuthenticationFailure] {

  private val validator = new PasswordHashValidator[USER] { override val passwordService = passService }

  override def authenticate(credentials: Request[AnyContent]): Future[Validation[AuthenticationFailure,USER]] = {
    val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(OauthFailure("Todo.. fix this message")))
    LoginForms.loginForm.bindFromRequest()(credentials).fold(
      errors => future { fail },
      { case (id: String, password: String) =>
        val uid = users.strAsId(id)
        users.find(uid).map { _.fold {
          val fail: Validation[AuthenticationFailure,USER] = Failure(AuthenticationServiceFailure(IdentityNotFound(uid)))
          Logger.debug("[reactivesecurity] Userpass Authentication Failed -- Id Not Found: "+uid)
          fail
          } { user => validator.validate(user,IdPass(id,password)) }
        }
      }
    )
  }
}