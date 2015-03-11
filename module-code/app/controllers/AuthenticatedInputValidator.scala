package reactivesecurity.controllers

////merge??
//import reactivesecurity.core.User.UsingID
//import reactivesecurity.core.Failures._
//
//import concurrent.{ExecutionContext, Future}
//import play.api.mvc.RequestHeader
//
//abstract class AuthenticatedInputValidator[User <: UsingID] extends InputValidator[RequestHeader,User,AuthenticationFailure] {
//  def validate(req: RequestHeader)(implicit ec: ExecutionContext): Future[Either[AuthenticationFailure,User]]
//
//  override def apply(in: RequestHeader)(implicit ec: ExecutionContext): Future[Either[AuthenticationFailure,User]] = {
//    validate(in).map { result =>
//      result.map {
//    }.getOrElse {
//      Future(Left(AuthenticationTokenNotFound))
//    }
//  }
//}
