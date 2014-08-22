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
package reactivesecurity.core.providers

import reactivesecurity.core.Failures._
import reactivesecurity.core._
import reactivesecurity.core.User.{UserService, UsingID}
import play.api.libs.ws.WS
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import LinkedInProvider._
import reactivesecurity.core.util.{Oauth1,OauthUserData}
import scala.concurrent.Future
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuthCalculator

case class LinkedInProvider[USER <: UsingID](service: UserService[USER]) extends OAuth1Provider[USER](service) {

  override def providerId = LinkedInProvider.LinkedIn

  def fill(accessToken: RequestToken, serviceInfo: ServiceInfo): Future[Option[OauthUserData]] = {
    WS.url(LinkedInProvider.Api).sign(OAuthCalculator(serviceInfo.key,accessToken)).get().map { response =>
      val me = response.json
      (me \ ErrorCode).asOpt[Int] match {
        case Some(error) => {
          val message = (me \ Message).asOpt[String]
          val requestId = (me \ RequestId).asOpt[String]
          val timestamp = (me \ Timestamp).asOpt[String]
          Logger.error(
            "Error retrieving information from LinkedIn. Error code: %s, requestId: %s, message: %s, timestamp: %s"
              format(error, message, requestId, timestamp)
          )
          None
        }
        case _ => {
          val userId = (me \ Id).as[String]
          val firstName = (me \ FirstName).asOpt[String].getOrElse("")
          val lastName = (me \ LastName).asOpt[String].getOrElse("")
          val fullName = (me \ FormattedName).asOpt[String].getOrElse("")
          val email = (me \ Email).asOpt[String].getOrElse("")
          val avatarUrl = (me \ PictureUrl).asOpt[String]
          Some(OauthUserData(LinkedInProvider.LinkedIn,userId,firstName,lastName,fullName,email,Oauth1(accessToken)))
        }
      }
    }
  }
}

object LinkedInProvider {
  val Api = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,formatted-name,picture-url,email-address)?format=json"
  val LinkedIn = "linkedin"
  val ErrorCode = "errorCode"
  val Message = "message"
  val RequestId = "requestId"
  val Timestamp = "timestamp"
  val Id = "id"
  val FirstName = "firstName"
  val LastName = "lastName"
  val Email = "emailAddress"
  val FormattedName = "formattedName"
  val PictureUrl = "pictureUrl"
}