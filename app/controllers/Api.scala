package controllers
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc.Controller
import daos.SessionHeaderDao
import daos.SessionLogDao
import scala.concurrent.ExecutionContext.Implicits._
import models.ES
import daos.UserProfileDao
import scala.concurrent.Future
import play.api.libs.Crypto
import helpers.Log
import helpers.MyHelper._
import models.RaceCar
import daos.RaceCarDao

trait RaceCarApi extends Log {
  this: Controller with MySecured =>

  def ajaxRaceCarSave(id: String) = Authenticated().async { request =>
    request.body.asJson.flatMap {
      case js: JsObject =>
        (js ++ Json.obj("eml" -> request.username)).asOpt[RaceCar] map { rc =>
          //TODO: user id may be overrided by racecar
          if (request.isAdmin || rc.eml.contains(request.username)) {
            RaceCar.saveRaceCar(request.username, rc) map { rcSaved => Ok(Json.toJson(rcSaved)) }
          } else Future.successful(Unauthorized("unauthorized"))

        }
    } getOrElse (Future.successful(BadRequest("invalid")))
  }

  def ajaxRaceCarDelete(id: String) = Authenticated().async { request =>
    val q = Json.obj("id" -> id)
    RaceCarDao.findFirstT(q) map {
      case Some(rc) => if (rc.eml.contains(request.username)) {
        RaceCarDao.remove(q, firstMatchOnly = true)
        Ok(q)
      } else Unauthorized("not the owner")
      case None => BadRequest("invalid")
    }
  }

  private implicit def listToJsArray[T](l: List[T])(implicit fmt: Format[T]): JsArray = l.foldLeft(Json.arr())((acc, item) => acc ++ Json.arr(item))

  def ajaxRaceCarGet(id: String) = Authenticated().async { request =>
    val commonq = if (request.isAdmin) Json.obj() else Json.obj("eml" -> request.username)
    val q = if (id == "") { commonq } else { Json.obj("id" -> id) ++ commonq }
    RaceCarDao.findT(q) map { l => Ok(l: JsArray) }
  }
}

trait UserProfileApi extends Log {
  this: Controller with MySecured =>

  val updatePasswordHashIfPresent = ((__ \ "passwordHash").json.copyFrom((__ \ "pass").json.pick.map { pass => JsString(Crypto.sign(pass.as[String])) }) and (__ \ "pass").json.prune) reduce

  val removePass = (__ \ "pass").json.prune andThen (__ \ "passwordHash").json.prune

  /**
   * only user or admin is allowed to do save
   */
  def ajaxUserProfileSave(userId: String) = Authenticated().async { request =>
    if (request.isAdmin || userId == request.userId) {
      request.body.asJson map {
        case js: JsObject =>
          log.debug(s"ajaxUserProfileSave: js = $js")
          (js \ "pass").asOpt[JsValue] match {
            case Some(js1) =>
              val toSave = js.transform(updatePasswordHashIfPresent).get
              log.debug(toSave.toString)
              UserProfileDao.insert(toSave) map { r => Ok(r.transform(removePass).get) }
            case None =>
              log.debug("no pass")
              UserProfileDao.insert(js) map { r => Ok(r.transform(removePass).get) }
          }

      } getOrElse (Future.successful(BadRequest("invalid")))
    } else Future.successful(Unauthorized("unauthorized"))

  }

  def ajaxUserProfileDelete(userId: String) = Authenticated().async { request =>
    if (request.isAdmin || userId == request.userId) {
      val id = Json.obj("id" -> userId)
      UserProfileDao.remove(id, true) map { x => Ok(id) }
    } else Future.successful(Unauthorized("unauthorized"))
  }

  private implicit def listToJsArray[T](l: List[T])(implicit fmt: Format[T]): JsArray = l.foldLeft(Json.arr())((acc, item) => acc ++ Json.arr(Json.toJson(item).transform(removePass).get))

  def ajaxUserProfileGet(userId: String) = Authenticated().async { request =>
    val q = if (userId == "") { Json.obj() } else { Json.obj("id" -> userId) }
    UserProfileDao.findT(q) map { l => Ok(l: JsArray) }
  }
}

object Api extends Controller with MySecured with UserProfileApi with RaceCarApi {

  val removeUsedField = ((__ \ "default").json.prune andThen (__ \ "user").json.prune andThen (__ \ "profile").json.prune)

  val removeIdField = (__ \ "_id").json.prune andThen (__ \ "id").json.prune

  def ajaxSessionUpdate(sessionId: String) = Authenticated().async { implicit request =>
    val q = Json.obj("eml" -> request.username, "session" -> sessionId)
    val js = request.body.asJson.get

    var jsToUpdate = Json.obj("indexed" -> false)
    (js \ "sessionName").asOpt[String] map { v => jsToUpdate = jsToUpdate ++ Json.obj("sessionName" -> v) }

    for {
      updateOk <- SessionHeaderDao.update(q, js.transform(removeIdField).get)
      sessionUpdateOk <- SessionLogDao.update(q, jsToUpdate, multi = true)
    } yield {
      Ok(Json.obj("id" -> sessionId))
    }
  }

  /**
   * load the logs to delete, and remove in both header, logs, and the submit the delete query to backend elasticsearch server
   */
  def ajaxSessionDelete(sessionId: String) = Authenticated().async { implicit request =>
    val sid = request.getQueryString("session").getOrElse(sessionId)
    val q = Json.obj("eml" -> request.username, "session" -> sid)

    for {
      logs <- SessionLogDao.find(q)
      sessRemoveOk <- SessionLogDao.remove(q, firstMatchOnly = false)
      headerRemoveOk <- SessionHeaderDao.remove(q)
    } yield {

      logs.foreach { l =>
        val eml = (l \ "eml").as[String]
        val session = (l \ "session").as[String]
        ES.esClient.deleteByQuery("obddata", "torquelogs", s"+eml:$eml+session:$session")
      }

      Ok(Json.obj("id" -> sessionId))
    }
  }

  def ajaxSessionsGet = Authenticated().async { implicit request =>
    val q = Json.obj("eml" -> request.username)
    SessionHeaderDao.find(q) map { l => Ok(l.foldLeft(Json.arr())((acc, item) => acc ++ Json.arr(item.transform(removeUsedField).get))) }
  }

  def average[T](ts: Iterable[T])(implicit num: Numeric[T]) = {
    num.toDouble(ts.sum) / ts.size
  }

  def ajaxSessionDataGet(sessionId: String) = Authenticated().async { implicit request =>
    var q = Json.obj("eml" -> request.username, "session" -> sessionId)
    val startTime = request.getQueryString("startTime")
    val endTime = request.getQueryString("endTime")

    val cursor = SessionLogDao.coll.find(q).sort(Json.obj("time" -> 1)).cursor[JsObject]

    val js = cursor.collect[List]() map { l1 =>
      val l2 = l1.filter(j => !(j \ "profileName").asOpt[String].isDefined)
      if (l2.size > 2) {
        val timeSlots = l2.map { jsobj => (jsobj \ "time").as[String] }

        /*
         * default HK
         * 22°15′N 114°10′E
         */
        val head = startTime map (t => l2.filter(l => (l \ "time").as[String] >= t).head) getOrElse l2.head
        val timeStart = (head \ "time").as[String]
        val locStartLat = (head \ "kff1006").asOpt[String] getOrElse ("22.15")
        val locStartLng = (head \ "kff1005").asOpt[String] getOrElse ("114.10")

        val last = endTime map (t => l2.filter(l => (l \ "time").as[String] <= t).last) getOrElse l2.last
        val timeEnd = (last \ "time").as[String]
        val locEndLat = (last \ "kff1006").asOpt[String] getOrElse ("22.15")
        val locEndLng = (last \ "kff1005").asOpt[String] getOrElse ("114.10")

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