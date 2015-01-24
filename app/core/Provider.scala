package core

import core.Authentication.AuthenticationValidator
import core.Failures.AuthenticationFailure
import core.User.UsingID

trait Provider2[In, User <: UsingID] extends AuthenticationValidator[In,User,AuthenticationFailure] {
  def providerId: String
}