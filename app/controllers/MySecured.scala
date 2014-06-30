package controllers

import play.api.mvc.ActionBuilder
import play.api.mvc.WrappedRequest
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Results.Redirect
import play.api.mvc.SimpleResult

trait MySecured {
  class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

  object Authenticated extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
      request.session.get("email").map { username =>
        block(new AuthenticatedRequest(username, request))
      } getOrElse {
        Future.successful(Redirect(routes.Application.login))
      }
    }
  }

}