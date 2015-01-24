package reactivesecurity.core

import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID

trait Provider2[In, User <: UsingID] extends AuthenticationValidator[In,User,AuthenticationFailure] {
  def providerId: String
}