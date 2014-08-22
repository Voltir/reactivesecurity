/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core.providers

import play.api.libs.ws._
import play.api.libs.functional.syntax._
import play.api.{Application, Logger}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import reactivesecurity.core.Failures._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.util.{Oauth2, OauthUserData}
import reactivesecurity.core.OAuth2Provider
import scala.concurrent.Future

/**
 * A Google OAuth2 Provider
 */
case class GoogleProvider[USER <: UsingID](service: UserService[USER]) extends OAuth2Provider[USER](service) {
  val UserInfoApi = "https://www.googleapis.com/oauth2/v1/userinfo?access_token="
  val Error = "error"
  val Message = "message"
  val Type = "type"
  val Id = "id"
  val Name = "name"
  val GivenName = "given_name"
  val FamilyName = "family_name"
  val Picture = "picture"
  val Email = "email"

  override def providerId = GoogleProvider.Google

  import play.api.Play.current

  def fill(accessToken: String):  Future[Option[OauthUserData]] = {
    WS.url(UserInfoApi + accessToken).get().map { response =>
      val me = response.json
      (me \ Error).asOpt[JsObject] match {
        case Some(error) => {
          val message = (error \ Message).asOpt[String]
          val errorType = ( error \ Type).asOpt[String]
          Logger.error("[reactivesecurity] error retrieving profile information from Google. Error type = %s, message = %s"
            .format(errorType,message))
          None
        }
        case _ => {
          val userId = (me \ Id).as[String]
          val firstName = (me \ GivenName).asOpt[String].getOrElse("")
          val lastName = (me \ FamilyName).asOpt[String].getOrElse("")
          val fullName = (me \ Name).asOpt[String].getOrElse("")
          val avatarUrl = ( me \ Picture).asOpt[String].getOrElse("")
          val email = ( me \ Email).asOpt[String].getOrElse("")
          Some(OauthUserData(GoogleProvider.Google,userId,firstName,lastName,fullName,email,Oauth2(accessToken)))
        }
      }
    }
  }
}

object GoogleProvider {
  val Google = "google"
}
