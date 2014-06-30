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

object Application extends Controller with ProfileService with MySecured {

  def index = Authenticated { request =>
    Ok(views.html.index(request.username))
  }

  def login = Action { implicit request =>
    Ok(views.html.login())
  }

  def logout = Action {
    Redirect(routes.Application.login).withNewSession
  }

  case class LoginData(email: String)

  import play.api.data._
  import play.api.data.Forms._
  val loginForm = Form(mapping("email" -> nonEmptyText)(LoginData.apply)(LoginData.unapply))

  def checkLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(

      formWithErrors => {
        Future.successful(Redirect(routes.Application.login).flashing("msg" -> "email missing"))
      },

      loginData => {
        val email = loginData.email
        hasEmail(email) map { flag =>
          if (flag) {
            Redirect(routes.Application.index).withSession("email" -> email)
          } else {
            Redirect(routes.Application.login).flashing("msg" -> "email is invalid")
          }
        }
      })
  }

}

trait ProfileService extends MongoController { this: Controller =>

  def hasEmail(email: String): Future[Boolean] = {
    val q = Json.obj("eml" -> email)
    SessionHeaderDao.findFirst(q).map { _.isDefined }
  }

}