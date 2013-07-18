package reactivesecurity.defaults

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.UsingID
import reactivesecurity.core.providers.UserPasswordProvider

//TODO -- More providers
abstract class DefaultUserPasswordProvider[USER <: UsingID] extends UserPasswordProvider[USER] {
  override val credentialsValidator = new AlwaysValid[USER] //TODO Real Credential Validator
}

abstract class DefaultProviders[USER <: UsingID] extends Providers[USER] {
  //Implementors must implement an userPasswordProvider[ID,USER]
  //override val userPasswordProvider = ???
  override val authenticator = DefaultAuthenticator
}
