package reactivesecurity.core


import reactivesecurity.core.User.UsingID
import reactivesecurity.core.util.OauthUserData

object Failures {

  trait AuthenticationFailure

  abstract class UserServiceFailure extends AuthenticationFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

  case class IdentityNotFound[USER <: UsingID](info: String) extends UserServiceFailure

  case object ValidationFailure extends UserServiceFailure

  case class RequiresNewOauthUser(oauth: OauthUserData) extends AuthenticationFailure

  case object InvalidPassword extends AuthenticationFailure

  case object CredentialsNotFound extends AuthenticationFailure

  case object AuthenticationTokenNotFound extends AuthenticationFailure

  case class OauthFailure(reason: String) extends AuthenticationFailure

  case object OauthNoVerifier extends AuthenticationFailure

  case object OAuth2NoAccessCode extends AuthenticationFailure

  case class Oauth2ProviderConfigurationError(provider: String) extends AuthenticationFailure

  trait AuthorizationFailure

  case object NotAuthorized extends AuthorizationFailure
}
