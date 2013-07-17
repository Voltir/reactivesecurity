package reactivesecurity.defaults

import reactivesecurity.core.std.AuthenticatedInputValidator
import reactivesecurity.core.User.{RequiresUsers, UsingID}

abstract class DefaultInputValidator[ID, USER <: UsingID[ID]] extends AuthenticatedInputValidator[ID,USER] with RequiresUsers[ID,USER] {
  override val authenticator = DefaultAuthenticator
}
