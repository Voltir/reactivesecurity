package reactivesecurity.defaults

import reactivesecurity.core.std.AuthenticatedInputValidator
import reactivesecurity.core.User.{UserService, UsingID}

abstract class DefaultAuthentication[USER <: UsingID](val users: UserService[USER]) extends AuthenticatedInputValidator[USER] {
  override val authenticator = DefaultAuthenticator
}
