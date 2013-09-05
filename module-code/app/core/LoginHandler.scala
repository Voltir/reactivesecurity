/* TO BE DELETED
package reactivesecurity.core

import play.api.Play
import play.api.mvc._

import reactivesecurity.core.User.UsingID

object LoginHandler {

  val onLoginGoTo = "reactivesecurity.onLoginGoTo"

  def landingUrl = Play.current.configuration.getString(onLoginGoTo).getOrElse(
    Play.current.configuration.getString("application.context").getOrElse("/"))

  val OriginalUrlKey = "original-url"
}
*/
