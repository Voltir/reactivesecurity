package reactivesecurity.defaults

import scala.concurrent.{ExecutionContext, future}
import scalaz.{Validation, Success}

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.{ UsingID, CredentialValidator, UserService}
import reactivesecurity.core.providers.{IdPass, UserPasswordProvider}
import reactivesecurity.core.std.{StringFromString, AuthenticationFailure}

case class TodoUser(id: String) extends UsingID[String] {
}

object DefaultUserService extends InMemoryUserService[String,TodoUser]

object TodoCredentialsValidator extends CredentialValidator[TodoUser, IdPass[String], AuthenticationFailure] {
  def validate(user: TodoUser, credential: IdPass[String]) = Success(user)
}

object DefaultUserProvider extends UserPasswordProvider[String,TodoUser] {
  override val userService = DefaultUserService
  override val credentialsValidator = TodoCredentialsValidator
}

object DefaultProviders extends Providers[String,TodoUser] {
  override val str2id = StringFromString
  override val userPasswordProvider = DefaultUserProvider
  override val authenticator = DefaultAuthenticator
}
