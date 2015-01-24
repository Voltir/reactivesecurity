package core.util

import play.Play
import play.api.mvc.Call
import play.api.PlayException
import reactivesecurity.core.User.UsingID

object RoutesHelper {

  lazy val conf = play.api.Play.current.configuration
  lazy val exception = new PlayException("Required Configuration Missing: reactivesecurity.login",
    """Reactive Security requires the login class to be defined in the config file, for example:
       reactivesecurity.login = controllers.MyLogin""")

  def getReverseControllerName(): String = {
    conf.getString("reactivesecurity.login").map { text =>
      val parts = text.split('.')
      if(parts.size <= 0) throw exception
      val reverse = parts.take(parts.size -1).mkString + s".Reverse${parts(parts.size -1)}"
      reverse
    }.getOrElse {
      throw exception
    }
  }

  // ProviderController
  lazy val lc = Play.application().classloader().loadClass(getReverseControllerName())
  lazy val loginControllerMethods = lc.newInstance().asInstanceOf[{
    def authenticate(p: String): Call
    def associateProviderCallback(provider: String): Call
  }]

  def authenticate(provider:String): Call = loginControllerMethods.authenticate(provider)

  def associateProviderCallback(provider: String): Call = loginControllerMethods.associateProviderCallback(provider)
}
