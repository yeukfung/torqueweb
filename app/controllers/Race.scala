package controllers

import play.api.mvc.Controller
import models.UserProfile
import scala.concurrent.Future

object Race extends Controller with MySecured {

  def index = Authenticated(role = Some(UserProfile.ROLE_race)).async {
    Future.successful(Ok("TODO"))
  }
}