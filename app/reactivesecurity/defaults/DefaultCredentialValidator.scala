package reactivesecurity.defaults

import scalaz.{Failure, Validation, Success}
import scala.concurrent.{Future,future}

import reactivesecurity.core.User.{UsingID, CredentialValidator}
import reactivesecurity.core.providers.IdPass
import reactivesecurity.core.std.{InvalidPassword, AuthenticationFailure}
import reactivesecurity.core.{PasswordStore, PasswordInfo, PasswordHasher}


class AlwaysValid[USER <: UsingID] extends CredentialValidator[USER, IdPass[USER]] {
  override def validate(user: USER, credential: IdPass[USER]) = future { Success(user) }
}

abstract class HashValidator[USER <: UsingID] extends CredentialValidator[USER,IdPass[USER]] {
  val hasher: PasswordHasher
  val ps: PasswordStore[USER]

  override def validate(user: USER, credential: IdPass[USER]): Future[Validation[AuthenticationFailure,USER]] = {
    ps.find(credential.id).map { info: PasswordInfo =>
      if(hasher.matches(info,credential.password)) Success(user)
      else Failure(InvalidPassword)
    }.getOrElse {
      Failure(InvalidPassword)
    }
  }
}
