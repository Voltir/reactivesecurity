package reactivesecurity.defaults

import reactivesecurity.core.std.AuthenticatedInputValidator
import reactivesecurity.core.User.{AsID, UserService, UsingID}

abstract class DefaultAuthentication[USER <: UsingID](val users: UserService[USER]) extends AuthenticatedInputValidator[USER] { //with RequiresUsers[USER] {
  override val asID: AsID[USER]
  override val authenticator = DefaultAuthenticator
}
