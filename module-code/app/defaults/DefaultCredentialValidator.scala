package reactivesecurity.defaults

import scalaz.{Failure, Validation, Success}

import reactivesecurity.core.User.{UsingID, CredentialValidator}
import reactivesecurity.core.providers.IdPass
import reactivesecurity.core.std.{InvalidPassword, AuthenticationFailure}
import reactivesecurity.core.PasswordHasher

class AlwaysValid[USER <: UsingID] extends CredentialValidator[USER, IdPass[USER]] {
  override def validate(user: USER, credential: IdPass[USER]) = Success(user)
}

trait HashValidator[USER <: UsingID] extends CredentialValidator[USER,IdPass[USER]] {

  val hasher: PasswordHasher

  override def validate(user: USER, credential: IdPass[USER]): Validation[AuthenticationFailure,USER] = {
    if(hasher.matches(user.authenticationInfo,credential.password)) Success(user)
    else Failure(InvalidPassword())
  }
}