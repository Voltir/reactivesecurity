package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}


object User {

  trait UsingID[ID] {
    val id: ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait IdFromString[ID] {
    def apply(id: String): ID
  }

  trait UserService[ID,USER <: UsingID[ID],FAILURE] {
    def find(id: ID)(implicit ec: ExecutionContext): Future[Validation[FAILURE, USER]]
    def save(user: USER): Unit
  }

  trait CredentialValidator[USER <: UsingID[_], CRED, FAIL] {
    def validate(user: USER, credential: CRED): Validation[FAIL,USER]
  }
}