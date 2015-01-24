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

import play.api.{Logger, Play, Application}
import play.api.cache.Cache
import Play.current
import play.api.mvc.{Call, Results, Result, Request}


import reactivesecurity.core.Authentication.AuthenticationValidator
import reactivesecurity.core.Failures._
import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.util.{OauthAuthenticationHelper, OauthUserData}
import scala.concurrent.{ExecutionContext, Future}
import concurrent.ExecutionContext.Implicits.global
import scalaz.{Failure, Validation}
import play.api.libs.ws.WS
import play.api.libs.oauth.ServiceInfo
import core.util.RoutesHelper

/**
 * The Oauth2 details
 *
 * @param accessToken the access token
 * @param tokenType the token type
 * @param expiresIn the number of seconds before the token expires
 * @param refreshToken the refresh token
 */
case class OAuth2Info(
  accessToken: String,
  tokenType: Option[String] = None,
  expiresIn: Option[Int] = None,
  refreshToken: Option[String] = None
)

/**
 * Base class for all OAuth2 providers
 */
abstract class OAuth2ProviderTODODELETE[USER <: UsingID](userService: UserService[USER]) extends Provider[USER] {

  def fill(accessToken: String): Future[Option[OauthUserData]]

  override def providerId: String

  val maybeSettings = getSettings
  val secured = play.api.Play.current.configuration.getString("https.port").isDefined

  private def getSettings: Option[OAuth2Settings] = {
    val result = for {
      authorizationUrl  <- ConfHelper.loadProperty(OAuth2Settings.AuthorizationUrl,providerId)
      accessToken       <- ConfHelper.loadProperty(OAuth2Settings.AccessTokenUrl,providerId)
      clientId          <- ConfHelper.loadProperty(OAuth2Settings.ClientId,providerId)
      clientSecret      <- ConfHelper.loadProperty(OAuth2Settings.ClientSecret,providerId)
    } yield {
      val maybeScope = ConfHelper.loadProperty(OAuth2Settings.Scope,providerId)
      OAuth2Settings(authorizationUrl, accessToken, clientId, clientSecret, maybeScope)
    }
    result
  }

  private def getAccessToken[A](code: String, settings: OAuth2Settings, callback: String)(implicit request: Request[A]): Future[OAuth2Info] = {
    val params = Map(
      OAuth2Constants.ClientId -> Seq(settings.clientId),
      OAuth2Constants.ClientSecret -> Seq(settings.clientSecret),
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(callback)
    )
    WS.url(settings.accessTokenUrl).post(params).map { response =>
      val json = response.json
      if ( Logger.isDebugEnabled ) {
        Logger.debug("[securesocial] got json back [" + json + "]")
      }
      OAuth2Info(
        (json \ OAuth2Constants.AccessToken).as[String],
        (json \ OAuth2Constants.TokenType).asOpt[String],
        (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
        (json \ OAuth2Constants.RefreshToken).asOpt[String]
      )
    }
  }

  override def authenticate(credentials: Request[_])(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,USER]] = {
    credentials.queryString.get(OAuth2Constants.Code).flatMap(_.headOption).map { code =>
      val accessToken: Option[Future[OAuth2Info]] = for {
        settings <- maybeSettings
        sessionId <- credentials.session.get("sid")
        // todo: review this -> clustered environments
        originalState <- Cache.getAs[String](sessionId)
        currentState <- credentials.queryString.get(OAuth2Constants.State).flatMap(_.headOption)
        if originalState == currentState
      } yield {
        val callback = RoutesHelper.authenticate(providerId).absoluteURL(secured)(credentials)
        getAccessToken(code,settings,callback)(credentials)
      }

      accessToken.map { _.flatMap { token =>
        fill(token.accessToken)
        val oauth = fill(token.accessToken)
        oauth.flatMap { o =>
          if(o.isDefined) OauthAuthenticationHelper.finishAuthenticate(providerId,userService,o.get)
          else Future(Failure(OauthFailure("Could not retrieve oauth data")))
        }
      }}.getOrElse(Future(Failure(OauthFailure("Invalid OAuth2 Access Token"))))
    }.getOrElse(Future(Failure(OAuth2NoAccessCode)))
  }
}

case class OAuth2Settings(
  authorizationUrl: String,
  accessTokenUrl: String,
  clientId: String,
  clientSecret: String, scope: Option[String]
)

object OAuth2Settings {
  val AuthorizationUrl = "authorizationUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val ClientId = "clientId"
  val ClientSecret = "clientSecret"
  val Scope = "scope"
}

object OAuth2Constants {
  val ClientId = "client_id"
  val ClientSecret = "client_secret"
  val RedirectUri = "redirect_uri"
  val Scope = "scope"
  val ResponseType = "response_type"
  val State = "state"
  val GrantType = "grant_type"
  val AuthorizationCode = "authorization_code"
  val AccessToken = "access_token"
  val Error = "error"
  val Code = "code"
  val TokenType = "token_type"
  val ExpiresIn = "expires_in"
  val RefreshToken = "refresh_token"
  val AccessDenied = "access_denied"
}