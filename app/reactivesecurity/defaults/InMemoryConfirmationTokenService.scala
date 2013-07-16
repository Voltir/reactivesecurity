package reactivesecurity.defaults

import reactivesecurity.controllers.{ConfirmationToken, ConfirmationTokenService}
import scala.collection.mutable

object InMemoryConfirmationTokenService extends ConfirmationTokenService {

  private val tokens: mutable.Map[String,ConfirmationToken] = mutable.Map()

  def save(token: ConfirmationToken) = {
    tokens += (token.uuid -> token)
  }

  def find(uuid: String): Option[ConfirmationToken] = {
    tokens.get(uuid)
  }

}
