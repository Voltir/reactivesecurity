package reactivesecurity.core

import scalaz.Validation
import scala.concurrent.{ExecutionContext, Future}
import reactivesecurity.core.std.{AuthenticationFailure, UserServiceFailure}


object User {

  trait UsingID {
    type ID
    val id: ID
  }

  trait StringAsID[USER <: UsingID] {
    def strAsId(idStr: String): USER#ID
  }

  trait UserProvider[+USER] {
    def user: USER
  }

  trait UserService[USER <: UsingID] { this: StringAsID[USER] =>
    def find(id: USER#ID)(implicit ec: ExecutionContext): Future[Option[USER]]

    //Ignore invalid ids
    def findMany(ids: List[USER#ID])(implicit ec: ExecutionContext): Future[List[USER]]

    def idFromEmail(email: String): USER#ID

    def strAsId(idStr: String): USER#ID = this.strAsId(idStr)

    def save(user: USER)(implicit ec: ExecutionContext): Future[Boolean]
  }

}
