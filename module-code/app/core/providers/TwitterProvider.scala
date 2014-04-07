/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

import reactivesecurity.core._
import reactivesecurity.core.User.{UserService, UsingID}
import play.api.libs.ws.WS
import TwitterProvider._
import reactivesecurity.core.util.{Oauth1,OauthUserData}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuthCalculator


/**
 * A Twitter Provider
 */
case class TwitterProvider[USER <: UsingID](service: UserService[USER]) extends OAuth1Provider[USER](service) {
  override def providerId = TwitterProvider.Twitter

  def fill(accessToken: RequestToken, serviceInfo: ServiceInfo): Future[Option[OauthUserData]] = {
    WS.url(TwitterProvider.VerifyCredentials).sign(OAuthCalculator(serviceInfo.key,accessToken)).get().map { response =>
      val me = response.json
      val userId = (me \ Id).as[String]
      val name = (me \ Name).as[String]
      val profileImage = (me \ ProfileImage).asOpt[String]

      var first = name
      var last = ""
      val nameArray = name.split(' ')
      if(nameArray.length > 0) {
        first = nameArray(0)
        last = nameArray(nameArray.length-1)
      }
      Some(OauthUserData(TwitterProvider.Twitter,userId,first,last,name,EmailWorkaround,Oauth1(accessToken)))
    }
  }
}

object TwitterProvider {
  val VerifyCredentials = "https://api.twitter.com/1.1/account/verify_credentials.json"
  val Twitter = "twitter"
  val Id = "id_str"
  val Name = "name"
  val ProfileImage = "profile_image_url_https"
  val EmailWorkaround = "TWITTER_DOES_NOT_GIVE_EMAIL"
}