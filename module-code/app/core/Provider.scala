package reactivesecurity.core

import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Failures.AuthenticationFailure
import reactivesecurity.core.User.UsingID
import play.api.mvc.Request

trait Provider[USER <: UsingID] extends AuthenticationValidator[Request[_],USER,AuthenticationFailure] {
  def providerId: String
}

trait Provider2[In, User <: UsingID] extends AuthenticationValidator[In,User,AuthenticationFailure] {
  def providerId: String
}