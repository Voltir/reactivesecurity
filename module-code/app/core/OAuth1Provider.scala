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
import Play.current
import reactivesecurity.core.Authentication.AuthenticationService

import reactivesecurity.core.std._

import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{Future, future}
import concurrent.ExecutionContext.Implicits.global
import scalaz.Validation
import play.api.libs.oauth.OAuth
import scalaz.Failure
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.ConsumerKey


/**
 * Base class for all OAuth1 providers
 */

case class OAuth1Info(token: String, secret: String)

case class ThisDoesNotBelongHere(
  provider: String,
  userid: String,
  first: String,
  last: String,
  full: String,
  email: String)

abstract class OAuth1Provider[USER <: UsingID] extends AuthenticationService[Request[_],USER,AuthenticationFailure] {

  val todoMaybeValidator: ThisDoesNotBelongHere => USER

  def id: String

  def fill(oauthInfo: OAuth1Info, serviceInfo: ServiceInfo)(f: ThisDoesNotBelongHere => USER): Future[Validation[AuthenticationFailure,USER]]

  val serviceInfo: Option[ServiceInfo]= for {
    requestToken    <- Helper.loadProperty(OAuth1Provider.RequestTokenUrl,id)
    accessToken     <- Helper.loadProperty(OAuth1Provider.AccessTokenUrl,id)
    authorization   <- Helper.loadProperty(OAuth1Provider.AuthorizationUrl,id)
    consumerKey     <- Helper.loadProperty(OAuth1Provider.ConsumerKey,id)
    consumerSecret  <- Helper.loadProperty(OAuth1Provider.ConsumerSecret,id)
  } yield {
    ServiceInfo(requestToken,accessToken,authorization, ConsumerKey(consumerKey, consumerSecret))
  }

  val maybeService: Option[OAuth] =  serviceInfo.map { info => OAuth(info, use10a = true) }

  override def authenticate(credentials: Request[_]): Future[Validation[AuthenticationFailure,USER]] = {
    def fail(errTxt: String): Validation[AuthenticationFailure,USER] = Failure(OauthFailure(errTxt))
    credentials.queryString.get("oauth_verifier").map { seq =>
      val verifier = seq.head
      // 2nd step in the oauth flow, we have the access token in the cache, we need to
      // swap it for the access token
      val result = for {
        service <- maybeService
        cacheKey <- credentials.session.get(OAuth1Provider.CacheKey)
        requestToken <- Cache.getAs[RequestToken](cacheKey)
      } yield {
        service.retrieveAccessToken(RequestToken(requestToken.token, requestToken.secret), verifier) match {
          case Right(token) =>
            // the Cache api does not have a remove method.  Just set the cache key and expire it after 1 second for
            // now.
            Cache.set(cacheKey, "", 1)
            fill(OAuth1Info(token.token, token.secret),service.info)(todoMaybeValidator)
          case Left(oauthException) => {
            Logger.error("[reactivesecurity] error retrieving access token", oauthException)
            future { fail(oauthException.getMessage) }
          }
        }
      }
      result.getOrElse(future { fail("Invalid Provider or RequestToken") })
    }.getOrElse(future { Failure(OauthNoVerifier()) })
  }
}

object Helper {
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
