package reactivesecurity.controllers

//import play.api.mvc.RequestHeader
//import reactivesecurity.core.{AuthenticationToken, AuthenticatorService}
//import reactivesecurity.core.User.UsingID
//import reactivesecurity.core.Failures._
//import concurrent.{ExecutionContext, Future}
//import scalaz.{Failure, Validation}

//abstract class AuthenticatedInputValidator[User <: UsingID] extends InputValidator[RequestHeader,User,AuthenticationFailure] {
//  val authService: AuthenticatorService[User,AuthenticationFailure]
//  def extractAuthenticationToken(req: RequestHeader): Option[AuthenticationToken]
//
//  override def apply(in: RequestHeader)(implicit ec: ExecutionContext): Future[Validation[AuthenticationFailure,User]] = {
//    extractAuthenticationToken(in).map { token =>
//      authService.get(token)
//    }.getOrElse {
//      Future(Failure(AuthenticationTokenNotFound))
//    }
//  }
//}

