package controllers

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.json.Reads._
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import am.libs.es.ESClient
import daos.SessionHeaderDao
import models.ES
import play.api.i18n.Messages
import play.api.libs.Crypto
import models.UserProfile
import helpers.Log

object Application extends Controller with MySecured with Log {

  def index = Authenticated() { implicit request =>
    Ok(views.html.index(request.username))
  }

  def login = Action { implicit request =>
    Ok(views.html.login())
  }

  def logout = Action {
    Redirect(routes.Application.login).withNewSession
  }

  case class LoginData(email: String, password: String)

  import play.api.data._
  import play.api.data.Forms._
  val loginForm = Form(mapping("email" -> nonEmptyText, "password" -> nonEmptyText)(LoginData.apply)(LoginData.unapply))

  def checkLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(

      formWithErrors => {
        Future.successful(Redirect(routes.Application.login).flashing("msg" -> Messages("login.required")))
      },

      loginData => {
        val email = loginData.email
        val pass = loginData.password
        val signedPass = Crypto.sign(pass)

        log.debug(s"email = $email, signedPass = $signedPass")
        UserProfile.getByLoginData(email, signedPass) map { optProfile =>
          if (optProfile.isDefined) {
            val redirectUrl = optProfile.get.role match {
              case UserProfile.ROLE_race => routes.Race.index
              case UserProfile.ROLE_admin => routes.Admin.index
              case _ => routes.Application.index
            }
            Redirect(redirectUrl).withSession(("email" -> email), ("role" -> optProfile.get.role), ("userId" -> optProfile.get.id.get))

          } else {
            Redirect(routes.Application.login).flashing("msg" -> Messages("login.invalid"))
          }
        }
      })
  }

}

