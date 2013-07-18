package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}
import reactivesecurity.core.std.{AuthenticationFailure, UserServiceFailure}


object User {

  trait UsingID {
    type ID
    val id: ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[USER <: UsingID] {
    def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure, USER]]
    def save(user: USER): Unit
  }

  trait CredentialValidator[USER <: UsingID, CRED] {
    def validate(user: USER, credential: CRED): Future[Validation[AuthenticationFailure,USER]]
  }

  trait RequiresUsers[USER <: UsingID] {
    val users: UserService[USER]
    def str2ID(inp: String): USER#ID
  }

}
