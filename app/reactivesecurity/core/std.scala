package reactivesecurity.core

object std {
  trait AuthenticationFailure

  abstract class UserServiceFailure extends AuthenticationFailure

  case class IdentityNotFound[Identity](username: Identity) extends UserServiceFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

}
