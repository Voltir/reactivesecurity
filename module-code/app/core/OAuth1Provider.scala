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
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuth, ServiceInfo}
import play.api.{ Logger, Play}

import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.{Redirect,Ok}
import Play.current
import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.std._
import reactivesecurity.core.std.AuthenticationServiceFailure
import reactivesecurity.core.User.{UserService, UsingID}
import scala.concurrent.{Future, future}
import concurrent.ExecutionContext.Implicits.global
import scalaz.{Failure, Validation}
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

abstract class Oauth1ProviderMK2[USER <: UsingID] extends AuthenticationService[Request[AnyContent],USER,AuthenticationFailure] {
  val service: OAuth
  val todoMaybeValidator: ThisDoesNotBelongHere => USER

  def fill(oauthInfo: OAuth1Info, serviceInfo: ServiceInfo)(f: ThisDoesNotBelongHere => USER): Future[Validation[AuthenticationFailure,USER]]

  override def authenticate(credentials: Request[AnyContent]): Future[Validation[AuthenticationFailure,USER]] = {
    val fail: Validation[AuthenticationFailure,USER] = Failure(OauthFailure("Todo.. fix this message"))
    credentials.queryString.get("oauth_verifier").map { seq =>
      val verifier = seq.head
      // 2nd step in the oauth flow, we have the access token in the cache, we need to
      // swap it for the access token
      val wat = for {
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
            future { fail }
          }
        }
      }
      wat.getOrElse(future { fail })
    }.getOrElse(future { Failure(OauthNoVerifier()) })
  }
}

abstract class OAuth1Provider[USER <: UsingID] /*extends AuthenticationService[RequestHeader,USER,AuthenticationFailure]*/ {
  //val serviceInfo = createServiceInfo(propertyKey)
  val serviceInfo = ServiceInfo(
    "https://api.linkedin.com/uas/oauth/requestToken",
    "https://api.linkedin.com/uas/oauth/accessToken",
    "https://api.linkedin.com/uas/oauth/authenticate",
    ConsumerKey("r3qupq7ohgp4", "CiGEuduaOanl52HT"))
  val service = OAuth(serviceInfo, use10a = true)

  //def authMethod = AuthenticationMethod.OAuth1
  /*
  def createServiceInfo(key: String): ServiceInfo = {
    val result = for {
      requestTokenUrl <- loadProperty(OAuth1Provider.RequestTokenUrl) ;
      accessTokenUrl <- loadProperty(OAuth1Provider.AccessTokenUrl) ;
      authorizationUrl <- loadProperty(OAuth1Provider.AuthorizationUrl) ;
      consumerKey <- loadProperty(OAuth1Provider.ConsumerKey) ;
      consumerSecret <- loadProperty(OAuth1Provider.ConsumerSecret)
    } yield {
      ServiceInfo(requestTokenUrl, accessTokenUrl, authorizationUrl, ConsumerKey(consumerKey, consumerSecret))
    }

    if ( result.isEmpty ) {
      throwMissingPropertiesException()
    }
    result.get
  }
  */

  def fill(oauthInfo: OAuth1Info, serviceInfo: ServiceInfo)(f: ThisDoesNotBelongHere => USER): Future[Validation[AuthenticationFailure,USER]]

  //override def authenticate(request: RequestHeader): Future[Validation[AuthenticationFailure,USER]] = {
  def rawrStab[A](callbackUrl: String)(f: ThisDoesNotBelongHere => USER)(implicit request: Request[A]): Future[Either[Result, USER]] = {
    if ( request.queryString.get("denied").isDefined ) {
      // the user did not grant access to the account
    }

    request.queryString.get("oauth_verifier").map[Future[Either[Result, USER]]] { seq =>
      val verifier = seq.head
      val stabwat: Future[Either[Result, USER]] = future { Left(Ok("An Error Occured and fix this")) }
      // 2nd step in the oauth flow, we have the access token in the cache, we need to
      // swap it for the access token
      val wat = for {
        cacheKey <- request.session.get(OAuth1Provider.CacheKey)
        requestToken <- Cache.getAs[RequestToken](cacheKey)
      } yield {

        service.retrieveAccessToken(RequestToken(requestToken.token, requestToken.secret), verifier) match {
          case Right(token) =>
            // the Cache api does not have a remove method.  Just set the cache key and expire it after 1 second for
            // now.
            Cache.set(cacheKey, "", 1)
            fill(OAuth1Info(token.token, token.secret),serviceInfo)(f).map { _.fold(
              fail => Left(Ok("Handle 2nd stage fail to create a user")),
              (user: USER) => Right(user)
            )}
          case Left(oauthException) => {
            Logger.error("[reactivesecurity] error retrieving access token", oauthException)
            stabwat
          }
        }
      }
    wat.getOrElse(stabwat)
    }.getOrElse {
      // the oauth_verifier field is not in the request, this is the 1st step in the auth flow.
      // we need to get the request tokens
      //val callbackUrl = RoutesHelper.authenticate(id).absoluteURL(IdentityProvider.sslEnabled)
      if ( Logger.isDebugEnabled ) {
        Logger.debug("[reactivesecurity] callback url = " + callbackUrl)
      }
      service.retrieveRequestToken(callbackUrl) match {
        case Right(accessToken) =>  {
          val cacheKey = UUID.randomUUID().toString
          val redirect = Redirect(service.redirectUrl(accessToken.token)).withSession(request.session +
            (OAuth1Provider.CacheKey -> cacheKey))
          Cache.set(cacheKey, accessToken, 600) // set it for 10 minutes, plenty of time to log in
          future { (Left(redirect)) }
        }
        case Left(e) => {
          Logger.error("[reactivesecurity] error retrieving request token", e)
          future { Left(Ok("Todo -- Handle error case when retriving request token")) }
        }
      }
    }
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
