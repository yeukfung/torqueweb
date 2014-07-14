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
import views.MyRequestHeader
import scala.concurrent.Await
import helpers.DefaultDur
import scala.concurrent.ExecutionContext.Implicits._

trait MySecured extends DefaultDur {

  class AuthenticatedRequest[A](val username: String, val userId: String, val isAdmin: Boolean, request: Request[A]) extends WrappedRequest[A](request)

  def alwaysTrue(userId: String, eml: String, role: String) = true

  def Authenticated(role: Option[String] = None, authorization: (String, String, String) => Boolean = alwaysTrue) = new ActionBuilder[AuthenticatedRequest] {

    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
      val requiredRole = role getOrElse UserProfile.ROLE_normal
      val result = for {
        userId <- request.session.get("userId")
        eml <- request.session.get("email")
        role <- request.session.get("role")
      } yield {
        val isAdmin = role == UserProfile.ROLE_admin
        if ((requiredRole == role) ||
          (isAdmin) ||
          (requiredRole == UserProfile.ROLE_normal && role == UserProfile.ROLE_race)) {

          if (authorization(userId, eml, role)) {
            block(new AuthenticatedRequest(eml, userId, isAdmin, request))
          } else
            Future.successful(Results.Unauthorized("authorization failed"))
        } else
          Future.successful(Results.Unauthorized("role does not match"))
      }

      result getOrElse {
        Future.successful(Redirect(routes.Application.login))
      }
    }
  }

  implicit def toMyRequestHeader[A](implicit req: AuthenticatedRequest[A]): MyRequestHeader = Await.result(UserProfile.getProfileByEmail(req.username).map { u => MyRequestHeader(u) }, dur)
}