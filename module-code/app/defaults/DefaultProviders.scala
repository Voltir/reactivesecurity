package reactivesecurity.defaults

import reactivesecurity.controllers.Providers
import reactivesecurity.core.User.UsingID

abstract class DefaultProviders[USER <: UsingID] extends Providers[USER] {
  override val authenticator = DefaultAuthenticator
}
