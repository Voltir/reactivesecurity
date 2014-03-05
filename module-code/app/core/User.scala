package reactivesecurity.core

import scala.concurrent.{ExecutionContext, Future}

object User {

  trait UsingID {
    type ID
    val id: ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[USER <: UsingID] {
    def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[USER]]
    def findByEmail(email: String)(implicit ec: ExecutionContext): Future[Option[USER]]
  }
}