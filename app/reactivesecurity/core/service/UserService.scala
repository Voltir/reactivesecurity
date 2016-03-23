package reactivesecurity.core.service

import com.softwaremill.macwire.MacwireMacros._
import scala.concurrent.{ExecutionContext, Future}

//trait HasID {
//  type ID
//  val id: ID
//}

trait Identifiable[User] {
  type ID
  def idFor(u: User): ID
}

//trait UserService[User <: HasID] {
trait UserService[User] {
  def findByProvider(provider: String, identifier: String)(implicit ex: ExecutionContext): Future[Option[User]]
}
