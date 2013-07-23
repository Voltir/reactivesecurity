package reactivesecurity.defaults

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.providers.UserPasswordProvider

//TODO -- More providers
class BCryptUserPasswordProvider[USER <: UsingID](users: UserService[USER]) extends UserPasswordProvider[USER](users) {
  override val credentialsValidator = new HashValidator[USER] { override val hasher = BCryptHasher }
}

abstract class DefaultProviders[USER <: UsingID] extends Providers[USER] {
  override val authenticator = DefaultAuthenticator
}
