package reactivesecurity.controllers

import scalaz.Scalaz._
import play.api.mvc._

import play.api.templates.Html
import scala.concurrent.future
import reactivesecurity.core.LoginHandler
import play.api.http.HeaderNames
import play.api.data._
import play.api.data.Forms._
import reactivesecurity.core.User.{IdFromString, UsingID, UserService}
import reactivesecurity.core.std.{UserServiceFailure, AuthenticationFailure}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import java.util.UUID
import play.api.i18n.Messages


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
  //def delete(uuid: String): Unit
}

abstract class Login[ID,USER <: UsingID[ID]] extends Controller {

  val loginHandler: LoginHandler[USER]
  val userService: UserService[ID,USER,UserServiceFailure]
  val confirmationTokenService: ConfirmationTokenService
  val asId: IdFromString[ID]

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

    withRefererAsOriginalUrl(Ok(loginHandler.getLoginPage(request)))
  }

  def startRegistration = Action { implicit request =>
    withRefererAsOriginalUrl(Ok(loginHandler.getRegistrationStartPage(request)))
  }

  def handleStartRegistration = Action { implicit request =>
    Async {
      Login.registrationForm.bindFromRequest.fold(
        errors => future { println("TODO REGISTRATION: "+errors); BadRequest(loginHandler.getRegistrationStartPage(request)) },
        { case (email,pass) =>
          val id = asId(email)
          userService.find(id).map { validation =>
            validation.map { user =>
              //Ok("Already registered TODO: " + user)
              println("Send already registered email")
              Redirect(loginHandler.afterRegistrationCall)//.flashing("success" -> Messages("ThankYouCheckEmail"), "email" -> email)
            }.getOrElse {
              val token = confirmationTokenService.createAndSaveToken(email,true)
              println("Send to email:" + token)
              Redirect(loginHandler.afterRegistrationCall).flashing("success" -> Messages("ThankYouCheckEmail"), "email" -> email)
            }
          }
        }
      )
    }
  }

  def executeForToken(confUUID: String, isSignUp: Boolean, f: ConfirmationToken => Result): Result = {
    confirmationTokenService.find(confUUID) match {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => {
        f(t)
      }
      case _ => {
        Redirect(loginHandler.startRegistrationCall).flashing("error" -> Messages("TODO Something about ConfirmationToken"))
      }
    }
  }

  def registration(confirmation: String) = Action { implicit request =>
    executeForToken(confirmation, true, { _ =>
       Ok(loginHandler.getRegistrationPage(request,confirmation))
    })
  }

  def handleRegistration(confirmation: String) =  Action { implicit request =>
    executeForToken(confirmation, true, { c =>
      loginHandler.userForm.bindFromRequest.fold(
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
          userService.save(user)
          //TODO confirmationTokenService.delete
          Ok("TODO -- Finished Registration")
        }
      )
    })
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

object Login {
  val registrationForm = Form[(String,String)](
    tuple(
      "userid" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )
}
