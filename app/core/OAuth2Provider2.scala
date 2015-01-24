
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

import java.net.URLEncoder
import java.util.UUID

import play.api.libs.ws.WS
import core.Failures._
import core.User.{UserService, UsingID}
import core.util.{OauthAuthenticationHelper, OauthUserData}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Failure, Validation}

trait OAuth2Service[In,Out] {

  def extract(in: In)(settings: OAuth2Settings): Option[(String,String)]

  def absoluteCallbackURL(in: In)(provider: String): String

  //Output is the identifier that will be used to retrieve the state value
  def storeSessionState(in: In)(state: String): String

  //should be possible to load the state variable from just the input
  def loadSessionState(in: In): Option[String]

  //sessionId is the output from storeSessionState
  def onRetreiveAccessCode(in: In)(accessUrl: String, sessionId: String): Future[Out]

  def retrieveAccessCode(in: In)(providerId: String, settings: OAuth2Settings): Future[Out] = {
    // There's no code in the request, this is the first step in the oauth flow
    val state = UUID.randomUUID().toString
    val sessionId = storeSessionState(in)(state)
    var params = List(
      (OAuth2Constants.ClientId, settings.clientId),
      (OAuth2Constants.RedirectUri, absoluteCallbackURL(in)(providerId)),
      (OAuth2Constants.ResponseType, OAuth2Constants.Code),
      (OAuth2Constants.State, state))
    settings.scope.foreach( s => { params = (OAuth2Constants.Scope, s) :: params })
    val url = settings.authorizationUrl +
      params.map( p => p._1 + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("?", "&", "")
    onRetreiveAccessCode(in)(url,sessionId)
  }

}

/**
 * Base class for all OAuth2 providers
 */
abstract class OAuth2Provider[In,Out,User <: UsingID](userService: UserService[User])
    extends Provider2[In,User] {

  val oauth2Service: OAuth2Service[In,Out]

  def fill(accessToken: String)(implicit ctx: ExecutionContext): Future[Option[OauthUserData]]

  override def providerId: String

  lazy val maybeSettings = getSettings

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

  override def authenticate(credentials: In)
                           (implicit ctx: ExecutionContext): Future[Validation[AuthenticationFailure,User]] = {
    val accessToken = extractAccessToken(credentials)(providerId,maybeSettings.get)
    if(accessToken.isDefined) {
      accessToken.get.flatMap { token =>
        val oauth = fill(token.accessToken)
        oauth.flatMap { o =>
          if (o.isDefined) OauthAuthenticationHelper.finishAuthenticate(providerId, userService, o.get)
          else Future(Failure(OauthFailure("Could not retrieve oauth data")))
        }
      }
    } else {
      Future(Failure(OAuth2NoAccessCode))
    }
  }

  private def extractAccessToken(in: In)
                                (providerId: String, settings: OAuth2Settings)
                                (implicit ctx: ExecutionContext): Option[Future[OAuth2Info]] = {
    val accessToken: Option[Future[OAuth2Info]] = for {
      (code,currentState) <- oauth2Service.extract(in)(settings)
      originalState <- oauth2Service.loadSessionState(in)
      if originalState == currentState
    } yield {
      fetchAccessToken(code, settings, oauth2Service.absoluteCallbackURL(in)(providerId))
    }
    accessToken
  }

  private def fetchAccessToken(code: String, settings: OAuth2Settings, callback: String)
                              (implicit ctx: ExecutionContext): Future[OAuth2Info] = {
    import play.api.Play.current
    val params = Map(
      OAuth2Constants.ClientId -> Seq(settings.clientId),
      OAuth2Constants.ClientSecret -> Seq(settings.clientSecret),
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(callback)
    )

    WS.url(settings.accessTokenUrl).post(params).map { response =>
      val json = response.json
      OAuth2Info(
        (json \ OAuth2Constants.AccessToken).as[String],
        (json \ OAuth2Constants.TokenType).asOpt[String],
        (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
        (json \ OAuth2Constants.RefreshToken).asOpt[String]
      )
    }
  }
}