package reactivesecurity.defaults

import reactivesecurity.core.std.{StringFromString, AuthenticatedInputValidator}
import reactivesecurity.core.User.UsingID

class DefaultInputValidator[USER <: UsingID[String]] extends AuthenticatedInputValidator[String,USER] {
  override val authenticator = DefaultAuthenticator
  override val users = new InMemoryUserService[String,USER]
  val str2id = StringFromString
}
