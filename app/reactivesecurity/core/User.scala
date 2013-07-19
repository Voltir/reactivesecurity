package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}
import reactivesecurity.core.std.{AuthenticationFailure, UserServiceFailure}


object User {

  //TODO: Rename to Subject?
  //TODO: Create an AuthInfo trait?
  //TODO: Allow multiple providers to map to one USER / SUBJECT?
  trait UsingID {
    type ID
    val id: ID
    val authenticationInfo: PasswordInfo
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[USER <: UsingID] {
    def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure, USER]]
    def save(user: USER): Unit
  }

  trait CredentialValidator[USER <: UsingID, CRED] {
    def validate(user: USER, credential: CRED): Validation[AuthenticationFailure,USER]
  }

  trait RequiresUsers[USER <: UsingID] {
    val users: UserService[USER]
    def str2ID(inp: String): USER#ID
  }

}
