package controllers

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.json.Reads._
import scala.concurrent.ExecutionContext.Implicits._

object Application extends Controller with MongoController with ProfileService {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def login = Action { implicit request =>
    Ok(views.html.login())
  }

  def home = Action {
    Ok("home")
  }

  def logout = Action {
    Redirect(routes.Application.login).withNewSession
  }

  case class LoginData(email: String)

  import play.api.data._
  import play.api.data.Forms._
  val loginForm = Form(
    mapping("email" -> nonEmptyText)(LoginData.apply)(LoginData.unapply))

  def checkLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(

      formWithErrors => {
        Future.successful(Redirect(routes.Application.login).flashing("msg" -> "email missing"))
      },

      loginData => {
        val email = loginData.email
        hasEmail(email) map { flag =>
          if (flag) {
            Redirect(routes.Application.home).withSession("email" -> email)
          } else {
            Redirect(routes.Application.login).flashing("msg" -> "email is invalid")
          }
        }
      })
  }
}

trait ProfileService {
  this: MongoController =>
  def collHeaders: JSONCollection = db.collection[JSONCollection]("sessionheaders")

  def hasEmail(email: String): Future[Boolean] = {
    val q = Json.obj("eml" -> email)
    val cursor = collHeaders.find(q).cursor[JsObject]
    cursor.collect[List](1).map { _.headOption.isDefined }
  }

}