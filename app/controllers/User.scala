package controllers

import play.api.mvc.Controller
import scala.concurrent.Future

object User extends Controller with MySecured {

  def profile = Authenticated().async { implicit request =>
    Future.successful(Ok(views.html.user.profile()))
  }
}