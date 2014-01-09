package reactivesecurity.core

import scala.concurrent.{ExecutionContext, Future}

object User {

  trait UsingID {
    type ID
    def identity: ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[USER <: UsingID] {
    /*this: StringAsID[USER] =>*/
    def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[USER]]

    //Ignore invalid ids
    def findMany(ids: List[USER#ID])(implicit ec: ExecutionContext): Future[List[USER]]

    def idFromEmail(email: String): USER#ID

    def save(user: USER)(implicit ec: ExecutionContext): Future[Boolean]
  }
}