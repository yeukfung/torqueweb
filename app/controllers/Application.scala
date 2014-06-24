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
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Application extends Controller with MongoController with ProfileService {

  def index = Authenticated { request =>
    Ok(views.html.index(request.username))
  }

  val esSvr = "http://localhost:9200/"
  def esRedirect(urlpath: String) = Authenticated.async { implicit request =>
    val url = esSvr + urlpath;
    println(url);
    val resp = request.method.toLowerCase() match {
      case "post" => WS.url(url).post(request.body.asJson.getOrElse(Json.obj()))
      case "put" => WS.url(url).put(request.body.asJson.getOrElse(Json.obj()))
      case "delete" => WS.url(url).delete()
      case _ => 
//        if (request.path.contains("""dashboard"""))
//        WS.url(url + s"?eml=${request.username}").get
//      else 
        WS.url(url).get
    }

    resp.map(r => Ok(r.json))
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
            Redirect(routes.Application.index).withSession("email" -> email)
          } else {
            Redirect(routes.Application.login).flashing("msg" -> "email is invalid")
          }
        }
      })
  }

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

  val removeUsedField = ((__ \ "default").json.prune andThen (__ \ "user").json.prune)

  def ajaxSessionsGet = Authenticated.async { implicit request =>
    val q = Json.obj("eml" -> request.username)
    val cursor = collHeaders.find(q).sort(Json.obj("time" -> 1)).cursor[JsObject]
    cursor.collect[List]() map { l => Ok(l.foldLeft(Json.arr())((acc, item) => acc ++ Json.arr(item.transform(removeUsedField).get))) }
  }

  /**
   * <======
   * kff1270 => 1003.91
   * kd => 0.0
   * kff1238 => 13.1
   * k5 => 63.0
   * k11 => 17.254902
   * kff1249 => 14.51921
   * kff125d => 2.974441
   * kff1222 => -0.020925445
   * kff1202 => -9.333603
   * ke => 9.5
   * kff1005 => 114.10090495
   * kff1203 => 0.0
   * v => 7
   * kff1001 => 0.0
   * kc => 746.0
   * kff1006 => 22.37103085
   * id => cca6e349887eab3ace1aed9d0cb877b7
   * kf => 35.0
   * kff1220 => -0.102504045
   * session => 1402964543624
   * kb => 37.0
   * kff1007 => 0.0
   * time => 1402964591489
   * k4 => 38.039215
   * eml => yeukfung@gmail.com
   * kff1221 => 0.9938011
   * kff1223 => 0.002540223
   * -->
   *
   */

  def average[T](ts: Iterable[T])(implicit num: Numeric[T]) = {
    num.toDouble(ts.sum) / ts.size
  }

  def ajaxSessionDataGet(sessionId: String) = Authenticated.async { implicit request =>
    var q = Json.obj("eml" -> request.username, "session" -> sessionId)
    val startTime = request.getQueryString("startTime")
    val endTime = request.getQueryString("endTime")

    val cursor = collSessionLogs.find(q).sort(Json.obj("time" -> 1)).cursor[JsObject]
    val js = cursor.collect[List]() map { l1 =>
      val l2 = l1.filter(j => !(j \ "profileName").asOpt[String].isDefined)
      if (l2.size > 2) {
        val timeSlots = l2.map { jsobj => (jsobj \ "time").as[String] }

        val head = startTime map (t => l2.filter(l => (l \ "time").as[String] >= t).head) getOrElse l2.head
        val timeStart = (head \ "time").as[String]
        val locStartLat = (head \ "kff1006").as[String]
        val locStartLng = (head \ "kff1005").as[String]

        val last = endTime map (t => l2.filter(l => (l \ "time").as[String] <= t).last) getOrElse l2.last
        val timeEnd = (last \ "time").as[String]
        val locEndLat = (last \ "kff1006").as[String]
        val locEndLng = (last \ "kff1005").as[String]

        val l = l2.filter { js =>
          val t = (js \ "time").as[String]
          t >= timeStart && t <= timeEnd
        }

        val engineLoadList = l.map { jsobj => (jsobj \ "k4").asOpt[String].getOrElse("-999") } filter (_ != "-999")
        val engineLoadAvg = if (engineLoadList.size > 0) average(engineLoadList.map { _.toDouble }) else -1

        val engineRPMList = l.map { jsobj => (jsobj \ "kc").asOpt[String].getOrElse("-999") } filter (_ != "-999")
        val engineRPMAvg = if (engineRPMList.size > 0) average(engineRPMList.map { _.toDouble }) else -1

        val KPLList = l.map { jsobj => (jsobj \ "kff1203").asOpt[String].getOrElse("-999") } filter (_ != "-999")
        val KPLAvg = if (KPLList.size > 0) average(KPLList.map { _.toDouble }) else -1

        val speedList = l.map { jsobj => (jsobj \ "kd").asOpt[String].getOrElse("-999") } filter (s => s != "-999" && s != "0.0")
        val speedAvg = if (speedList.size > 0) average(speedList.map { _.toDouble }) else -1
        //println(speedList)
        val speedMax = if (speedList.size > 0) speedList.map(_.toDouble).max else -1

        Json.obj(
          "start" -> timeStart,
          "end" -> timeEnd,
          "startLoc" -> Json.obj("lat" -> locStartLat, "lng" -> locStartLng),
          "endLoc" -> Json.obj("lat" -> locEndLat, "lng" -> locEndLng),
          "timeslots" -> timeSlots,
          "engineLoadAvg" -> engineLoadAvg,
          "engineRPMAvg" -> engineRPMAvg,
          "kplAvg" -> KPLAvg,
          "speedAvg" -> speedAvg,
          "speedMax" -> speedMax)
      } else {
        Json.obj();
      }

    }

    js map (Ok(_))

  }
}

trait ProfileService {
  this: MongoController =>
  def collHeaders: JSONCollection = db.collection[JSONCollection]("sessionheaders")
  def collSessionLogs: JSONCollection = db.collection[JSONCollection]("sessionlogs")

  def hasEmail(email: String): Future[Boolean] = {
    val q = Json.obj("eml" -> email)
    val cursor = collHeaders.find(q).cursor[JsObject]
    cursor.collect[List](1).map { _.headOption.isDefined }
  }

}