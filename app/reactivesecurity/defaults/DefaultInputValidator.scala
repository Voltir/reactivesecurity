package reactivesecurity.defaults

import reactivesecurity.core.std.{StringAsId, AuthenticatedInputValidator}
import reactivesecurity.core.User.UserWithIdentity

class DefaultInputValidator[USER <: UserWithIdentity[String]] extends AuthenticatedInputValidator[String,USER] {
  val str2id = StringAsId
  override val authenticator = DefaultAuthenticator
  override val users = new DefaultUserService[String,USER]
}
