package controllers

import play.api.mvc.ActionBuilder
import play.api.mvc.WrappedRequest
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Results.Redirect
import play.api.mvc.SimpleResult
import play.api.mvc.Action
import models.UserProfile
import play.api.mvc.Results

trait MySecured {

  class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

  def Authenticated(role: Option[String] = None) = new ActionBuilder[AuthenticatedRequest] {

    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
      val requiredRole = role getOrElse UserProfile.ROLE_normal
      val result = for {
        eml <- request.session.get("email")
        role <- request.session.get("role")
      } yield {
        if ((requiredRole == role) ||
          (role == UserProfile.ROLE_admin) ||
          (requiredRole == UserProfile.ROLE_normal && role == UserProfile.ROLE_race))
          block(new AuthenticatedRequest(eml, request))
        else
          Future.successful(Results.Unauthorized("role does not match"))
      }

      result getOrElse {
        Future.successful(Redirect(routes.Application.login))
      }
    }
  }

}