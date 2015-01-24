package object controllers {

  import play.api.mvc.{Request, Result}
  import play.api.http.HeaderNames

  import scalaz.Scalaz._

  def withRefererAsOriginalUrl[A](result: Result)(implicit request: Request[A]): Result = {
    val origUrl = "original-url"
    request.session.get(origUrl) match {
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
            request.session + (origUrl -> refererUri))
        } | result
      }
    }
  }
}
