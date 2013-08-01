package reactivesecurity.defaults

import reactivesecurity.controllers.Login
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.User.{UserService, UsingID}

abstract class DefaultLogin[USER <: UsingID] extends Login[USER] {
  override val userService: UserService[USER]
  override val passwordService: PasswordService[USER]
  override val confirmationTokenService = InMemoryConfirmationTokenService
}