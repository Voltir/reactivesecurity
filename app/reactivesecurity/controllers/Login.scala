package reactivesecurity.controllers

import scalaz.Scalaz._
import play.api.mvc._
import org.joda.time.DateTime
import java.util.UUID

import scala.concurrent.{Future,future}
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.http.HeaderNames
import play.api.i18n.Messages
import play.api.data._
import play.api.data.Forms._

import reactivesecurity.core.User.{RequiresUsers,UsingID}
import reactivesecurity.core.std.{UserServiceFailure}
import reactivesecurity.core.LoginHandler
import play.api.templates.Html

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

abstract class Login[ID,USER <: UsingID[ID]] extends Controller with RequiresUsers[ID,USER] {

  val confirmationTokenService: ConfirmationTokenService

  def getLoginPage(request: RequestHeader): Html
  def getRegistrationStartPage(request: RequestHeader): Html
  def getRegistrationPage(request: RequestHeader, confirmation: String): Html

  def registrationStartRedirect: Call
  def registrationAfterRedirect: Call
  def registrationDoneRedirect: Call

  def userForm: Form[USER]

  def login = Action { implicit request =>
    /*
    val to = ProviderController.landingUrl
    if ( SecureSocial.currentUser.isDefined ) {
      // if the user is already logged in just redirect to the app
      if ( Logger.isDebugEnabled() ) {
        Logger.debug("User already logged in, skipping login page. Redirecting to %s".format(to))
      }
      Redirect( to )
    } else {
      import com.typesafe.plugin._
      SecureSocial.withRefererAsOriginalUrl(Ok(use[TemplatesPlugin].getLoginPage(request, UsernamePasswordProvider.loginForm)))
    }
    */

    withRefererAsOriginalUrl(Ok(getLoginPage(request)))
  }

  def startRegistration = Action { implicit request =>
    withRefererAsOriginalUrl(Ok(getRegistrationStartPage(request)))
  }

  def handleStartRegistration = Action { implicit request =>
    Async {
      LoginForms.registrationForm.bindFromRequest.fold(
        errors => future { println("TODO REGISTRATION: "+errors); BadRequest(getRegistrationStartPage(request)) },
        { case (email,pass) =>
          val id = string2Id(email)
          users.find(id).map { validation =>
            validation.map { user =>
              println("Send already registered email")
            }.getOrElse {
              val token = confirmationTokenService.createAndSaveToken(email,true)
              println("Send to email:" + token)
            }
            Redirect(registrationAfterRedirect).flashing("success" -> Messages("ThankYouCheckEmail"), "email" -> email)
          }
        }
      )
    }
  }

  def executeForToken(confirmation: String, isSignUp: Boolean, f: ConfirmationToken => Result): Future[Result] = future {
    confirmationTokenService.find(confirmation) match {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => {
        f(t)
      }
      case _ => {
        Redirect(registrationStartRedirect).flashing("error" -> Messages("TODO Something about ConfirmationToken"))
      }
    }
  }

  def registration(confirmation: String) = Action { implicit request =>
    Async {
      executeForToken(confirmation, true, { _ =>
         Ok(getRegistrationPage(request,confirmation))
      })
    }
  }

  def handleRegistration(confirmation: String) =  Action { implicit request =>
    Async {
      executeForToken(confirmation, true, { c =>
        userForm.bindFromRequest.fold(
          errors => { println(errors); Ok("TODO -- handleRegistration -- ERRORS") },
          user => {
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
            users.save(user)
            //TODO Mailer
            confirmationTokenService.delete(confirmation)
            Redirect(registrationDoneRedirect).flashing("success" -> Messages("reactivesecurity.registrationDone"))
          }
        )
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
