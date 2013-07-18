package reactivesecurity.defaults

import reactivesecurity.core.std.AuthenticatedInputValidator
import reactivesecurity.core.User.{RequiresUsers, UsingID}

abstract class DefaultInputValidator[USER <: UsingID] extends AuthenticatedInputValidator[USER] with RequiresUsers[USER] {
  override val authenticator = DefaultAuthenticator
}
