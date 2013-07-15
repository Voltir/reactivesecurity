package reactivesecurity.defaults

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.{IdService, CredentialValidator, UserService}
import reactivesecurity.core.providers.{IdPass, UserPasswordProvider}
import reactivesecurity.core.std.AuthenticationFailure
import scala.concurrent.{ExecutionContext, future}
import scalaz.{Validation, Success}

case class TodoUser(id: String)

object TodoIdService extends IdService[String] {
  override def idFromString(id: String) = id
}

object TodoDefaultUserService extends UserService[String, AuthenticationFailure, TodoUser] {
  def findById(id: String)(implicit ec: ExecutionContext) = future { Success(TodoUser(id)) }
}

object TodoCredentialsValidator extends CredentialValidator[TodoUser, IdPass[String], AuthenticationFailure] {
  def validate(user: TodoUser, credential: IdPass[String]) = Success(user)
}

object DefaultUserProvider extends UserPasswordProvider[String,TodoUser] {
  override val userService = TodoDefaultUserService
  override val credentialsValidator = TodoCredentialsValidator
}

object DefaultAuthenticator extends Providers[String,TodoUser] {
  override val idService = TodoIdService
  override val userPasswordProvider = DefaultUserProvider
}
