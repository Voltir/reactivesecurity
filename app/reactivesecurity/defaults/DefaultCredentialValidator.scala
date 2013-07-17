package reactivesecurity.defaults

import reactivesecurity.core.User.{UsingID, CredentialValidator}
import reactivesecurity.core.providers.IdPass
import reactivesecurity.core.std.AuthenticationFailure
import scalaz.Success


class AlwaysValid[ID,USER <: UsingID[ID]] extends CredentialValidator[USER, IdPass[ID], AuthenticationFailure] {
  override def validate(user: USER, credential: IdPass[ID]) = Success(user)
}
