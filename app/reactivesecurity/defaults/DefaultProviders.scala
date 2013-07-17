package reactivesecurity.defaults

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.UsingID
import reactivesecurity.core.providers.UserPasswordProvider

//TODO -- More providers
abstract class DefaultUserPasswordProvider[ID,USER <: UsingID[ID]] extends UserPasswordProvider[ID,USER] {
  override val credentialsValidator = new AlwaysValid[ID,USER] //TODO Real Credential Validator
}

abstract class DefaultProviders[ID,USER <: UsingID[ID]] extends Providers[ID,USER] {
  //Implementors must implement an userPasswordProvider[ID,USER]
  //override val userPasswordProvider = ???
  override val authenticator = DefaultAuthenticator
}
