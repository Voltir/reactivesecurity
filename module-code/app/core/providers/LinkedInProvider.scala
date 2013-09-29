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

import reactivesecurity.core.OAuth1Info
import reactivesecurity.core.std.OauthFailure
import reactivesecurity.core.std.{OauthFailure, AuthenticationFailure}
import reactivesecurity.core._
import reactivesecurity.core.ThisDoesNotBelongHere
import reactivesecurity.core.User.UsingID
import play.api.libs.oauth._
import play.api.libs.ws.WS
import play.api.Logger
import LinkedInProvider._
import scala.concurrent.Future
import scalaz.Validation
import concurrent.ExecutionContext.Implicits.global
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuthCalculator
import scalaz.Success
import play.api.libs.oauth.ConsumerKey
import scalaz.Failure
import scala.Some
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuthCalculator
import scalaz.Success
import scalaz.Failure
import scala.Some


class LinkedInProviderMK2[USER <: UsingID](f: ThisDoesNotBelongHere => USER) extends OAuth1Provider[USER] {
  /*val serviceInfo = ServiceInfo(
    "https://api.linkedin.com/uas/oauth/requestToken",
    "https://api.linkedin.com/uas/oauth/accessToken",
    "https://api.linkedin.com/uas/oauth/authenticate",
    ConsumerKey("r3qupq7ohgp4", "CiGEuduaOanl52HT"))
  override val service = OAuth(serviceInfo, use10a = true)*/

  override def id = LinkedInProvider.LinkedIn

  override val todoMaybeValidator = f

  def fill(oauthInfo: OAuth1Info, serviceInfo: ServiceInfo)(f: ThisDoesNotBelongHere => USER): Future[Validation[AuthenticationFailure,USER]] = {
    WS.url(LinkedInProvider.Api).sign(OAuthCalculator(serviceInfo.key,
      RequestToken(oauthInfo.token, oauthInfo.secret))).get().map { response =>

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
          Failure(OauthFailure(message.getOrElse("Unknown Failure")))
        }
        case _ => {
          val userId = (me \ Id).as[String]
          val firstName = (me \ FirstName).asOpt[String].getOrElse("")
          val lastName = (me \ LastName).asOpt[String].getOrElse("")
          val fullName = (me \ FormattedName).asOpt[String].getOrElse("")
          val email = (me \ Email).asOpt[String].getOrElse("")
          val avatarUrl = (me \ PictureUrl).asOpt[String]
          Success(f(ThisDoesNotBelongHere(LinkedInProvider.LinkedIn,userId,firstName,lastName,fullName,email)))
        }
      }
    }
  }
}
/**
 * A LinkedIn Provider

class LinkedInProvider[USER <: UsingID] extends OAuth1Provider[USER] {


  //override def id = LinkedInProvider.LinkedIn

  def fill(oauthInfo: OAuth1Info, serviceInfo: ServiceInfo)(f: ThisDoesNotBelongHere => USER): Future[Validation[AuthenticationFailure,USER]] = {
    WS.url(LinkedInProvider.Api).sign(OAuthCalculator(serviceInfo.key,
      RequestToken(oauthInfo.token, oauthInfo.secret))).get().map { response =>

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
          Failure(OauthFailure(message.getOrElse("Unknown")))
        }
        case _ => {
          val userId = (me \ Id).as[String]
          val firstName = (me \ FirstName).asOpt[String].getOrElse("")
          val lastName = (me \ LastName).asOpt[String].getOrElse("")
          val fullName = (me \ FormattedName).asOpt[String].getOrElse("")
          val email = (me \ Email).asOpt[String].getOrElse("")
          val avatarUrl = (me \ PictureUrl).asOpt[String]
          Success(f(ThisDoesNotBelongHere("linkedin",userId,firstName,lastName,fullName,email)))
        }
      }
    }
  }
}
 */

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
