package reactivesecurity.core

import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.service.HasID

trait Provider2[In, User <: HasID] extends AuthenticationValidator[In,User,AuthenticationFailure] {
  def providerId: String
}