package reactivesecurity.core

import reactivesecurity.core.Password._
import reactivesecurity.core.providers.EmailPass
import reactivesecurity.core.Failures._
import reactivesecurity.core.service.Identifiable
import scala.concurrent.{ExecutionContext, Future}

object Credentials {

  trait CredentialValidator[User, Credential] {
    def validate(user: User, credential: Credential)
                (implicit ec: ExecutionContext, id: Identifiable[User]): Future[Either[AuthenticationFailure,User]]
  }

  trait PasswordHashValidator[User] extends CredentialValidator[User,EmailPass] {
    def passwordService: PasswordService[Identifiable[User]#ID]

    override def validate(user: User, credential: EmailPass)
                         (implicit ec: ExecutionContext, id: Identifiable[User]): Future[Either[AuthenticationFailure, User]] = {
      passwordService.find(id.idFor(user)).map {
        case Some(pass) if passwordService.hasher.matches(pass, credential.password) => Right(user)
        case Some(_) => Left(InvalidPassword)
        case None => Left(CredentialsNotFound)
      }
    }
  }
}