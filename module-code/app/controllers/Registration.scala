package reactivesecurity.controllers

import scala.concurrent.{Future,future}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import java.util.UUID

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import reactivesecurity.core.Password.PasswordService
import reactivesecurity.core.User.{UserService, UsingID}

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

trait Registration[USER <: UsingID] extends Controller {
  val userService: UserService[USER]
  val passwordService: PasswordService[USER]
  val confirmationTokenService: ConfirmationTokenService

  def onStartSignUp(request: RequestHeader, error: Option[String]): SimpleResult
  def onFinishSignUp(request: RequestHeader,cheatToken: Option[String]): SimpleResult //TODO -- Don't send this to the client... (Better yet, don't handle registration at all!)
  def onStartCompleteRegistration(request: RequestHeader, confirmation: String, id: USER#ID): SimpleResult
  def onCompleteRegistration[A](confirmation: String, id: USER#ID)(implicit request: Request[A]): (Option[USER],SimpleResult)

  def startRegistration = Action { implicit request =>
    withRefererAsOriginalUrl(onStartSignUp(request,None))
  }

  def handleStartRegistration = Action.async { implicit request =>
    LoginForms.registrationForm.bindFromRequest.fold(
      errors => future { println("TODO REGISTRATION: "+errors); onStartSignUp(request,None) },
      { case (email,pass) =>
        val id = userService.idFromEmail(email)
        userService.find(id).map { validation =>
         val maybeToken = validation.map { user =>
            println("Send already registered email")
            None
          }.getOrElse {
            val token = confirmationTokenService.createAndSaveToken(email,true)
            val passInfo = passwordService.hasher.hash(pass)
            passwordService.save(id,passInfo)
            //TODO remember to delete token (+ password if timed out)
            println("Send to email: " + token)
            Some(token)
          }
          onFinishSignUp(request,maybeToken)
        }
      }
    )
  }

  def executeForToken[A](confirmation: String, isSignUp: Boolean, f: ConfirmationToken => SimpleResult)(implicit request: Request[A]): Future[SimpleResult] = future {
    confirmationTokenService.find(confirmation) match {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => {
        f(t)
      }
      case _ => {
        onStartSignUp(request,Some("TODO Something about ConfirmationToken"))
      }
    }
  }

  def registration(confirmation: String) = Action.async { implicit request =>
    executeForToken(confirmation, true, { c =>
      onStartCompleteRegistration(request,confirmation,userService.idFromEmail(c.email))
    })
  }

  def handleRegistration(confirmation: String) =  Action.async { implicit request =>
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
      userMaybe.map { user =>
        confirmationTokenService.delete(confirmation)
        userService.save(user)
      }
      result
    })
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
