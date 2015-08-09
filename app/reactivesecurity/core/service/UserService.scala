package reactivesecurity.core.service

import com.softwaremill.macwire.MacwireMacros._
import scala.concurrent.{ExecutionContext, Future}

trait HasID {
  type ID
  val id: ID
}

trait UserService[User <: HasID] {
  def ec: ExecutionContext
  def findByProvider(provider: String, identifier: String): Future[Option[User]]
}
