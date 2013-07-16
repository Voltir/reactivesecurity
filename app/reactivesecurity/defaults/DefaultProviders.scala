package reactivesecurity.defaults

import scala.concurrent.{ExecutionContext, future}
import scalaz.{Validation, Success}

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.{ UserWithIdentity, CredentialValidator, UserService}
import reactivesecurity.core.providers.{IdPass, UserPasswordProvider}
import reactivesecurity.core.std.{StringAsId, AuthenticationFailure}

case class TodoUser(id: String) extends UserWithIdentity[String] {
  def rawId = id
}

object TodoDefaultUserService extends UserService[String,TodoUser,AuthenticationFailure] {
  def find(id: String)(implicit ec: ExecutionContext) = future { Success(TodoUser(id)) }
}

object TodoCredentialsValidator extends CredentialValidator[String, TodoUser, IdPass[String], AuthenticationFailure] {
  def validate(user: TodoUser, credential: IdPass[String]) = Success(user)
}

object DefaultUserProvider extends UserPasswordProvider[String,TodoUser] {
  override val userService = TodoDefaultUserService
  override val credentialsValidator = TodoCredentialsValidator
}

object DefaultProviders extends Providers[String,TodoUser] {
  override val str2id = StringAsId
  override val userPasswordProvider = DefaultUserProvider
  override val authenticator = DefaultAuthenticator
}
