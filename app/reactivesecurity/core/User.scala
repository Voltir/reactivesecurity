package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}


object User {

  trait UserProvider[+USR] {
    def user: USR
  }


  trait UserWithIdentity[ID] {
    def id: ID
    def rawId: String
  }

  trait IdService[ID] {
    def idFromString(id: String): ID
  }

  trait UserService[ID,FAIL,USR] {
    def findById(id: ID)(implicit ec: ExecutionContext): Future[Validation[FAIL, USR]]
  }

  trait CredentialValidator[USR, CRED, FAIL] {
    def validate(user: USR, credential: CRED): Validation[FAIL,USR]
  }
}