package reactivesecurity.core

import reactivesecurity.core.service.Identifiable
import scala.concurrent.{ExecutionContext, Future}

object Authentication {

  trait AuthenticationProcess[Input,Output,+User] {
    def authentication[A <: Input](block: User => Future[Output])
                                  (implicit ctx: ExecutionContext): A => Future[Output]
  }

  trait AuthenticationFailureHandler[-Input,-Failure,+Output] {
    def onAuthenticationFailure(in: Input)
                               (failure: Failure)
                               (implicit ctx: ExecutionContext): Future[Output]
  }

  trait AuthenticationValidator[-Credentials, User, +Failure] {
    def authenticate(credentials: Credentials)
                    (implicit ctx: ExecutionContext, id: Identifiable[User]): Future[Either[Failure,User]]
  }
}
