package reactivesecurity.defaults

import reactivesecurity.controllers.Login
import reactivesecurity.core.User.UsingID

abstract class DefaultLogin[USER <: UsingID] extends Login[USER] {
  override val authenticator = LocalCacheAuthenticator
}
