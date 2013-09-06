package reactivesecurity.defaults

import reactivesecurity.controllers.Registration
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.User.{UserService, UsingID}

abstract class InMemoryRegistration[USER <: UsingID] extends Registration[USER] {
  override val userService: UserService[USER]
  override val passwordService: PasswordService[USER]
  override val confirmationTokenService = InMemoryConfirmationTokenService
}