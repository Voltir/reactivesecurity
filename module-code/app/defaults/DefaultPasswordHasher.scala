package reactivesecurity.defaults

import play.api.Play
import org.mindrot.jbcrypt.BCrypt

import reactivesecurity.core.Password._

object BCryptHasher extends PasswordHasher {
  val roundsConfKey = "reactivesecurity.passwordHasher.bcrypt.rounds"
  override val id = "bcrypt"

  override def hash(plainPassword: String): PasswordInfo = {
    val rounds: Int = Play.current.configuration.getInt(roundsConfKey).getOrElse(10)
    PasswordInfo(id, BCrypt.hashpw(plainPassword, BCrypt.gensalt(rounds)))
  }

  override def matches(passwordInfo: PasswordInfo, supplied: String): Boolean = {
    BCrypt.checkpw(supplied, passwordInfo.password)
  }
}
