package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}
import reactivesecurity.core.std.UserServiceFailure


object User {

  trait UsingID[ID] {
    val id: ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[ID,USER <: UsingID[ID],FAILURE] {
    def find(id: ID)(implicit ec: ExecutionContext): Future[Validation[FAILURE, USER]]
    def save(user: USER): Unit
  }

  trait CredentialValidator[USER <: UsingID[_], CRED, FAIL] {
    def validate(user: USER, credential: CRED): Validation[FAIL,USER]
  }

  trait RequiresUsers[ID,USER <: UsingID[ID]] {
    val users: UserService[ID,USER,UserServiceFailure]
    def string2Id(inp: String): ID
  }

}