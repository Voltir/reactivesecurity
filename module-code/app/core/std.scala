package reactivesecurity.core


import reactivesecurity.core.User.UsingID

object Failures {

  trait AuthenticationFailure

  abstract class UserServiceFailure extends AuthenticationFailure

  case class AuthenticationServiceFailure[A](underlyingError: A) extends AuthenticationFailure

  case class IdentityNotFound[USER <: UsingID](userid: USER#ID) extends UserServiceFailure

  case object ValidationFailure extends UserServiceFailure

  case object InvalidPassword extends AuthenticationFailure

  case object CredentialsNotFound extends AuthenticationFailure

  case class OauthFailure(reason: String) extends AuthenticationFailure

  case object OauthNoVerifier extends AuthenticationFailure

  case object OAuth2NoAccessCode extends AuthenticationFailure

  trait AuthorizationFailure

  case object NotAuthorized extends AuthorizationFailure
  //Move this to "play" dir
  /*
  abstract class AuthenticatedInputValidator[USER <: UsingID] extends InputValidator[RequestHeader,USER,AuthenticationFailure] {
    val users: UserService[USER]
    val authenticator: Authenticator

    override def validateInput(in: RequestHeader)(implicit ec: ExecutionContext): Future[Validation[UserServiceFailure,USER]] = {
      val fail: Validation[UserServiceFailure,USER] = Failure(ValidationFailure)
      authenticator.find(in).fold(Future(fail)) {
        token => users.find(users.strAsId(token.uid)).map(_.fold(fail)(Success(_)))
      }
    }
  }
  */
}
