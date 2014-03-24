package reactivesecurity.core

import reactivesecurity.core.Password.{PasswordInfo, PasswordService}
import reactivesecurity.core.User.{UsingID, UserService}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime
import java.util.UUID

sealed trait InitialRegistrationResult
case class AlreadyRegistered(email: String) extends InitialRegistrationResult
case class ValidRegistration(token: RegistrationToken) extends InitialRegistrationResult
case class RegistrationError(email: String, error: Throwable) extends InitialRegistrationResult

case class RegistrationToken(uuid: String, email: String, creationTime: DateTime, expirationTime: DateTime, isSignUp: Boolean) {
  def isExpired = expirationTime.isBeforeNow
}

trait RegistrationTrait[USER <: UsingID] {
  val userService: UserService[USER]

  val TOKEN_DURATION = 48

  def checkInitialRegistration(email: String): Future[InitialRegistrationResult] = {
    userService.findByEmail(email).map { validation =>
      validation.map { user =>
        AlreadyRegistered(email)
      }.getOrElse {
        val token = createConfirmationToken(email,true)
        ValidRegistration(token)
      }
    }.recover { case err =>
      RegistrationError(email,err)
    }
  }

  def checkConfirmationToken(token: RegistrationToken, isSignUp: Boolean): Boolean = {
    !token.isExpired && token.isSignUp == isSignUp
  }

  //todo -- remove isSignUp :/
  private def createConfirmationToken(id: String, isSignUp: Boolean): RegistrationToken = {
    val uuid = UUID.randomUUID.toString
    val now = DateTime.now()
    val token = RegistrationToken(
      uuid,
      id,
      now,
      now.plusHours(TOKEN_DURATION),
      isSignUp
    )
    token
  }
}