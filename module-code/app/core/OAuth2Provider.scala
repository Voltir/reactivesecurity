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

import _root_.java.net.URLEncoder
import _root_.java.util.UUID
import play.api.{Logger, Play, Application}
import play.api.cache.Cache
import Play.current
import play.api.mvc.{Call, Results, Result, Request}


import reactivesecurity.core.Authentication.AuthenticationService
import reactivesecurity.core.std.{OauthFailure, OAuth2NoAccessCode, AuthenticationFailure}
import reactivesecurity.core.User.UsingID
import scala.concurrent.{Future, future}
import concurrent.ExecutionContext.Implicits.global
import scalaz.{Failure, Validation}
import play.api.libs.ws.WS
import play.api.libs.oauth.ServiceInfo

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
abstract class OAuth2Provider[USER <: UsingID] extends AuthenticationService[Request[_],USER,AuthenticationFailure] {

  val todoMaybeValidator: ThisDoesNotBelongHere => USER

  def fill(accessToken: String)(f: ThisDoesNotBelongHere => USER): Future[Validation[AuthenticationFailure,USER]]

  def id: String

  val maybeSettings = getSettings()

  //def authMethod = AuthenticationMethod.OAuth2

  private def getSettings(): Option[OAuth2Settings] = {
    val result = for {
      authorizationUrl  <- Helper.loadProperty(OAuth2Settings.AuthorizationUrl,id) ;
      accessToken       <- Helper.loadProperty(OAuth2Settings.AccessTokenUrl,id) ;
      clientId          <- Helper.loadProperty(OAuth2Settings.ClientId,id) ;
      clientSecret      <- Helper.loadProperty(OAuth2Settings.ClientSecret,id)
    } yield {
      val maybeScope = Helper.loadProperty(OAuth2Settings.Scope,id)
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
      //OAuth2Constants.RedirectUri -> Seq(RoutesHelper.authenticate(id).absoluteURL(IdentityProvider.sslEnabled))
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

  override def authenticate(credentials: Request[_]): Future[Validation[AuthenticationFailure,USER]] = {
    println("!!!################################")
    println(credentials.queryString)
    /*
    credentials.queryString.get(OAuth2Constants.Error).flatMap(_.headOption).map( error => {
      error match {
        case OAuth2Constants.AccessDenied => throw new AccessDeniedException()
        case _ =>
          Logger.error("[securesocial] error '%s' returned by the authorization server. Provider type is %s".format(error, id))
          throw new AuthenticationException()
      }
      throw new AuthenticationException()
    })
    */
    credentials.queryString.get(OAuth2Constants.Code).flatMap(_.headOption).map { code =>
      println("YAY CODE: "+code)
      val accessToken: Option[Future[OAuth2Info]] = for {
        settings <- maybeSettings
        sessionId <- credentials.session.get("sid");
        // todo: review this -> clustered environments
        originalState <- Cache.getAs[String](sessionId);
        currentState <- credentials.queryString.get(OAuth2Constants.State).flatMap(_.headOption)
        if originalState == currentState
      } yield {
        //??????????????
        implicit val req = credentials
        lazy val conf = play.api.Play.current.configuration
        lazy val pc = play.Play.application().classloader().loadClass("controllers.ReverseLogin")
        lazy val loginMethods = pc.newInstance().asInstanceOf[{
          def authenticate(p: String): Call
        }]
        //val callbackUrl = RoutesHelper.authenticate(id).absoluteURL(IdentityProvider.sslEnabled)
        val callback = loginMethods.authenticate(id).absoluteURL()
        //??????????????
        getAccessToken(code,settings,callback)
      }

      accessToken.map { _.flatMap { token =>
        fill(token.accessToken)(todoMaybeValidator)
      }}.getOrElse(future { Failure(OauthFailure("Invalid OAuth2 Access Token"))} )

      //future { Failure(OauthFailure("WIP")) }
    }.getOrElse(future { Failure(OAuth2NoAccessCode()) })
    /*
    credentials.queryString.get(OAuth2Constants.Code).flatMap(_.headOption) match {
      case Some(code) =>
        // we're being redirected back from the authorization server with the access code.
        val user = for (
          // check if the state we sent is equal to the one we're receiving now before continuing the flow.
          sessionId <- credentials.session.get(IdentityProvider.SessionId) ;
          // todo: review this -> clustered environments
          originalState <- Cache.getAs[String](sessionId) ;
          currentState <- credentials.queryString.get(OAuth2Constants.State).flatMap(_.headOption) if originalState == currentState
        ) yield {
          val accessToken = getAccessToken(code)
          val oauth2Info = Some(
            OAuth2Info(accessToken.accessToken, accessToken.tokenType, accessToken.expiresIn, accessToken.refreshToken)
          )
          //SocialUser(IdentityId("", id), "", "", "", None, None, authMethod, oAuth2Info = oauth2Info)
        }
        if ( Logger.isDebugEnabled ) {
          Logger.debug("[securesocial] user = " + user)
        }
        user match  {
          case Some(u) => Right(u)
          case _ => throw new AuthenticationException()
        }
      case None =>
        // There's no code in the request, this is the first step in the oauth flow
        val state = UUID.randomUUID().toString
        val sessionId = request.session.get(IdentityProvider.SessionId).getOrElse(UUID.randomUUID().toString)
        Cache.set(sessionId, state)
        var params = List(
          (OAuth2Constants.ClientId, settings.clientId),
          (OAuth2Constants.RedirectUri, RoutesHelper.authenticate(id).absoluteURL(IdentityProvider.sslEnabled)),
          (OAuth2Constants.ResponseType, OAuth2Constants.Code),
          (OAuth2Constants.State, state))
        settings.scope.foreach( s => { params = (OAuth2Constants.Scope, s) :: params })
        val url = settings.authorizationUrl +
          params.map( p => p._1 + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("?", "&", "")
        if ( Logger.isDebugEnabled ) {
          Logger.debug("[securesocial] authorizationUrl = %s".format(settings.authorizationUrl))
          Logger.debug("[securesocial] redirecting to: [%s]".format(url))
        }
        Left(Results.Redirect( url ).withSession(request.session + (IdentityProvider.SessionId, sessionId)))
    }
    */
    /*def fail(errTxt: String): Validation[AuthenticationFailure,USER] = Failure(OauthFailure(errTxt))
    credentials.queryString.get(OAuth2Constants.Code).map {
      future { fail("wip") }
    }.getOrElse(future { fail("wip") } )
    */
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
