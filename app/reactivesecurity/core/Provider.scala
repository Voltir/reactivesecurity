package reactivesecurity.core

import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Failures.AuthenticationFailure

trait Provider2[In, User] extends AuthenticationValidator[In,User,AuthenticationFailure] {
  def providerId: String
}