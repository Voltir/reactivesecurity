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
package core

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