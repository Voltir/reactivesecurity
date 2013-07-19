package reactivesecurity.defaults

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.UsingID
import reactivesecurity.core.providers.UserPasswordProvider

//TODO -- More providers
abstract class DefaultUserPasswordProvider[USER <: UsingID] extends UserPasswordProvider[USER] {
  override val credentialsValidator = new DefaultHashValidator[USER]
}

abstract class DefaultProviders[USER <: UsingID] extends Providers[USER] {
  override val authenticator = DefaultAuthenticator
}
