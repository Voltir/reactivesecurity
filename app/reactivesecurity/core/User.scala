package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}


object User {

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserWithIdentity[ID] {
    def id: ID
    def rawId: String
  }

  trait IdFromString[ID] {
    def apply(id: String): ID
  }

  trait IdStringFromInput[IN] {
    def apply(in: IN): Option[String]
  }

  trait StringFromId[ID] {
    def apply(id: ID): String
  }

  trait UserService[ID,USER <: UserWithIdentity[ID],FAILURE] {
    def find(id: ID)(implicit ec: ExecutionContext): Future[Validation[FAILURE, USER]]
  }

  trait CredentialValidator[ID, USER <: UserWithIdentity[ID], CRED, FAIL] {
    def validate(user: USER, credential: CRED): Validation[FAIL,USER]
  }
}