package reactivesecurity.controllers

import reactivesecurity.core.Password.PasswordService
import scalaz.Scalaz._
import play.api.mvc._
import org.joda.time.DateTime
import java.util.UUID

import scala.concurrent.{Future,future}
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.http.HeaderNames
import play.api.data._
import play.api.data.Forms._

import reactivesecurity.core.User.{UserService, UsingID}
import reactivesecurity.core.{Password, LoginHandler}
import play.api.mvc.Result

case class ConfirmationToken(uuid: String, email: String, creationTime: DateTime, expirationTime: DateTime, isSignUp: Boolean) {
  def isExpired = expirationTime.isBeforeNow
}

trait ConfirmationTokenService {
  val TOKEN_DURATION = 1

  def createAndSaveToken(id: String,isSignUp: Boolean): String = {
    val uuid = UUID.randomUUID.toString
    val now = DateTime.now()
    val token = ConfirmationToken(
      uuid,
      id,
      now,
      now.plusMinutes(TOKEN_DURATION),
      isSignUp
    )
    save(token)
    uuid
  }

  def save(token: ConfirmationToken)
  def find(uuid: String): Option[ConfirmationToken]
  def delete(uuid: String): Unit
}

trait Login[USER <: UsingID] extends Controller {
  val userService: UserService[USER]
  val passwordService: PasswordService[USER]
  val confirmationTokenService: ConfirmationTokenService

  def onUnauthorized(request: RequestHeader): Result
  def onLoginSucceeded(request: RequestHeader): PlainResult
  def onLogoutSucceeded(request: RequestHeader): PlainResult
  def onLogin(request: RequestHeader): Result

  def onStartSignUp(request: RequestHeader, error: Option[String]): Result
  def onFinishSignUp(request: RequestHeader): Result
  def onStartCompleteRegistration(request: RequestHeader, confirmation: String, id: USER#ID): Result
  def onCompleteRegistration[A](confirmation: String, id: USER#ID)(implicit request: Request[A]): (Option[USER],Result)

  def login = Action { implicit request =>
    withRefererAsOriginalUrl(onLogin(request))
  }

  def logout = Action { implicit request =>
    Ok("Todo")
  }

  def startRegistration = Action { implicit request =>
    withRefererAsOriginalUrl(onStartSignUp(request,None))
  }

  def handleStartRegistration = Action { implicit request =>
    Async {
      LoginForms.registrationForm.bindFromRequest.fold(
        errors => future { println("TODO REGISTRATION: "+errors); onStartSignUp(request,None) },
        { case (email,pass) =>
          val id = userService.idFromEmail(email)
          userService.find(id).map { validation =>
            validation.map { user =>
              println("Send already registered email")
            }.getOrElse {
              val token = confirmationTokenService.createAndSaveToken(email,true)
              val passInfo = passwordService.hasher.hash(pass)
              passwordService.save(id,passInfo)
              //TODO remember to delete token (+ password if timed out)
              println("Send to email: " + token)
            }
            onFinishSignUp(request)
          }
        }
      )
    }
  }

  def executeForToken[A](confirmation: String, isSignUp: Boolean, f: ConfirmationToken => Result)(implicit request: Request[A]): Future[Result] = future {
    confirmationTokenService.find(confirmation) match {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => {
        f(t)
      }
      case _ => {
        onStartSignUp(request,Some("TODO Something about ConfirmationToken"))
      }
    }
  }


  def registration(confirmation: String) = Action { implicit request =>
    Async {
      executeForToken(confirmation, true, { c =>
        onStartCompleteRegistration(request,confirmation,userService.idFromEmail(c.email))
      })
    }
  }


  def handleRegistration(confirmation: String) =  Action { implicit request =>
    Async {
      executeForToken(confirmation, true, { c =>

        ///loginHandler.getUserForm(asID(c.email)).bindFromRequest.fold(
        ///  errors => { println(errors); Ok("TODO -- handleRegistration -- ERRORS") },
        ///  user => {
            /*
            val saved = UserService.save(user)
            UserService.deleteToken(t.uuid)
            if ( UsernamePasswordProvider.sendWelcomeEmail ) {
              Mailer.sendWelcomeEmail(saved)
            }
            val eventSession = Events.fire(new SignUpEvent(user)).getOrElse(session)
            if ( UsernamePasswordProvider.signupSkipLogin ) {
              ProviderController.completeAuthentication(user, eventSession).flashing(Success -> Messages(SignUpDone))
            } else {
              Redirect(onHandleSignUpGoTo).flashing(Success -> Messages(SignUpDone)).withSession(eventSession)
            }
            */
        ///    users.save(user)
        ///    //TODO Mailer
        ///    confirmationTokenService.delete(confirmation)
        ///    Redirect(loginHandler.registrationDoneRedirect).flashing("success" -> Messages("reactivesecurity.registrationDone"))
        ///  }
        ///)
        //TODO: Rename "c.email" to something more ID like
        val (userMaybe,result)  = onCompleteRegistration(confirmation,userService.idFromEmail(c.email))(request) //TODO - Send Mail
        userMaybe.map {
          confirmationTokenService.delete(confirmation)
          userService.save(_)
        }
        result
      })
    }
  }

  def withRefererAsOriginalUrl[A](result: Result)(implicit request: Request[A]): Result = {
    request.session.get(LoginHandler.OriginalUrlKey) match {
      // If there's already an original url recorded we keep it: e.g. if s.o. goes to
      // login, switches to signup and goes back to login we want to keep the first referer
      case Some(_) => result
      case None => {
        request.headers.get(HeaderNames.REFERER).map { referer =>
        // we don't want to use the ful referer, as then we might redirect from https
        // back to http and loose our session. So let's get the path and query string only
          val idxFirstSlash = referer.indexOf("/", "https://".length())
          val refererUri = if (idxFirstSlash < 0) "/" else referer.substring(idxFirstSlash)
          result.withSession(
            request.session + (LoginHandler.OriginalUrlKey -> refererUri))
        } | result
      }
    }
  }
}

object LoginForms {
  val registrationForm = Form[(String,String)](
    tuple(
      "userid" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  val loginForm = Form[(String,String)](
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )
}