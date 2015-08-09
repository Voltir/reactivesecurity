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
package reactivesecurity.core

import _root_.java.util.UUID
import play.api.cache.Cache
import play.api.{ Logger, Play}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.{Redirect,Ok}
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.OAuth
import Play.current

import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Failures._
import reactivesecurity.core.util.{OauthAuthenticationHelper, OauthUserData}
import scala.concurrent.{ExecutionContext, Future}
import concurrent.ExecutionContext.Implicits.global
import scalaz.Validation
import scalaz.Failure


/**
 * Base class for all OAuth1 providers
 */

//case class OAuth1Info(token: String, secret: String)

//abstract class OAuth1Provider[USER <: UsingID](userService: UserService[USER]) extends Provider[USER] {
//
//  //val todoMaybeValidator: ThisDoesNotBelongHere => USER
//  //val helper: OauthAuthenticationHelper[USER]
//
//  def providerId: String
//
//  def fill(accessToken: RequestToken, serviceInfo: ServiceInfo): Future[Option[OauthUserData]]
//
//  val serviceInfo: Option[ServiceInfo]= for {
//    requestToken    <- ConfHelper.loadProperty(OAuth1Provider.RequestTokenUrl,providerId)
//    accessToken     <- ConfHelper.loadProperty(OAuth1Provider.AccessTokenUrl,providerId)
//    authorization   <- ConfHelper.loadProperty(OAuth1Provider.AuthorizationUrl,providerId)
//    consumerKey     <- ConfHelper.loadProperty(OAuth1Provider.ConsumerKey,providerId)
//    consumerSecret  <- ConfHelper.loadProperty(OAuth1Provider.ConsumerSecret,providerId)
//  } yield {
//    ServiceInfo(requestToken,accessToken,authorization, ConsumerKey(consumerKey, consumerSecret))
//  }
//
//  val maybeService: Option[OAuth] =  serviceInfo.map { info => OAuth(info, use10a = true) }
//
//  override def authenticate(credentials: Request[_])(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]] = {
//    def fail(errTxt: String): Validation[AuthenticationFailure,USER] = Failure(OauthFailure(errTxt))
//    credentials.queryString.get("oauth_verifier").map { seq =>
//      val verifier = seq.head
//      // 2nd step in the oauth flow, we have the access token in the cache, we need to
//      // swap it for the access token
//      val result = for {
//        service <- maybeService
//        cacheKey <- credentials.session.get(OAuth1Provider.CacheKey)
//        requestToken <- Cache.getAs[RequestToken](cacheKey)
//      } yield {
//        service.retrieveAccessToken(RequestToken(requestToken.token, requestToken.secret), verifier) match {
//          case Right(token) => {
//            Cache.remove(cacheKey)
//            val oauth = fill(RequestToken(token.token, token.secret),service.info)
//            oauth.flatMap { o =>
//              if(o.isDefined) OauthAuthenticationHelper.finishAuthenticate(providerId,userService,o.get)
//              else Future(fail("Could not retrieve oauth data"))
//            }
//          }
//          case Left(oauthException) => {
//            Logger.error("[reactivesecurity] error retrieving access token", oauthException)
//            Future(fail(oauthException.getMessage))
//          }
//        }
//      }
//      result.getOrElse(Future(fail("Invalid Provider or RequestToken")))
//    }.getOrElse(Future(Failure(OauthNoVerifier)))
//  }
//}

object ConfHelper {
  def propertyKey(id: String) = s"reactivesecurity.$id."

  def loadProperty(property: String, provider: String): Option[String] = {

    val result = play.api.Play.application.configuration.getString(propertyKey(provider) + property)
    if ( !result.isDefined ) {
      Logger.error("[reactivesecurity] Missing property " + property + " for provider " + provider)
    }
    result
  }
}

object OAuth1Provider {
  val CacheKey = "cacheKey"
  val RequestTokenUrl = "requestTokenUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val AuthorizationUrl = "authorizationUrl"
  val ConsumerKey = "consumerKey"
  val ConsumerSecret = "consumerSecret"
}
