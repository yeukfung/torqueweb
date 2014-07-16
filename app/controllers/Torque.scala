package controllers

import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json.Json
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits._
import am.libs.es.ESClient
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import java.util.Date
import java.text.SimpleDateFormat
import play.api.templates.Html
import reactivemongo.bson.BSONDocument
import play.api.Logger
import daos.SessionLogDao
import daos.SessionHeaderDao
import models.UserProfile
import models.ES
import helpers.Log

object Torque extends Controller with MongoController with Log {

  val pruneId = (__ \ "_id").json.prune
  //val updateKdToNumber = (__ \ "kd").json.update(__.read[JsObject].map { s => println(s); s })

  //  val pidMap = Map(
  //    "04" -> ("Engine Load", "%"),
  //    "0c" -> ("Engine RPM", "rpm"),
  //    "11" -> ("Throttle Position(Manifold)", "%"),
  //    "ff1203" -> ("Kilometers Per Litre(Instant)", "kpl"),
  //    "0d" -> ("Speed", "km/h"),
  //    "ff1249" -> ("Air Fuel Ratio(Measured)", "1"),
  //    "0f" -> ("Intake Air Temperature", "°C"),
  //    "0b" -> ("Intake Manifold Pressure", "kPa"),
  //
  //    "ff1258" -> ("CO2 in g/km (Average)", "g/km"),
  //    "05" -> ("Engine Coolant Temperature", "°C"),
  //    "ff1257" -> ("CO2 in g/km (Instantaneous)", "g/km"),
  //    "ff125d" -> ("Fuel flow rate/hour", "l/hr"),
  //    "2f" -> ("Fuel Level", "%"),
  //    "0a" -> ("Fuel Pressure", "kPa"),
  //    "10" -> ("Mass Air Flow Rate", "g/s"),
  //    "0e" -> ("Timing Advance", "°"),
  //    "ff1202" -> ("Turbo Boost & Vacuum Gauge", "psi"),
  //    "ff1238" -> ("Voltage (OBD Adapter)", "V"),
  //    "ff1273" -> ("Engine kW(At the wheels)", "kW"),
  //    "ff1270" -> ("Barometer (on Android device)", "mb"),
  //    "ff1225" -> ("Torque", "ft-lb"),
  //
  //    "ff124d" -> ("Air Fuel Ratio(Commanded)", "1"))

  val esClient = ES.esClient

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  def submitToElasticSearch = Action.async {

    // to double read
    def dr(fld: String) = (__ \ fld).readOpt[String].map(s => JsNumber(s.getOrElse("0.0").toDouble))

    val logToOBDDataJs = (
      (__ \ 'sessionName).json.copyFrom(((__ \ 'session).read[String]).map(s => JsString("trip - " + dateFormat.format(new Date(s.toLong))))) and
      (__ \ 'speed).json.copyFrom(dr("kd")) and
      (__ \ 'engineLoad).json.copyFrom(dr("k4")) and
      (__ \ 'engineRPM).json.copyFrom(dr("kc")) and
      (__ \ 'throttlePos).json.copyFrom(dr("k11")) and
      (__ \ 'kpl).json.copyFrom(dr("kff1203")) and
      (__ \ 'airFuelRatio).json.copyFrom(dr("kff1249")) and
      (__ \ 'intakeAirTemp).json.copyFrom(dr("kf")) and
      (__ \ 'intakeManifoldPressure).json.copyFrom(dr("kb")) and
      (__ \ 'engineCoolantTemp).json.copyFrom(dr("k5")) and
      (__ \ 'co2InGperKM).json.copyFrom(dr("kff1257")) and
      (__ \ 'fuelFlowRate).json.copyFrom(dr("kff125d")) and
      (__ \ 'fuelPressure).json.copyFrom(dr("ka")) and
      (__ \ 'massAirFlowRate).json.copyFrom(dr("k10")) and
      (__ \ 'timingAdvance).json.copyFrom(dr("ke")) and
      (__ \ 'vacuum).json.copyFrom(dr("kff1202")) and
      (__ \ 'voltage).json.copyFrom(dr("kff1238")) and
      (__ \ 'barometer).json.copyFrom(dr("kff1270")) and
      (__ \ 'torque).json.copyFrom(dr("kff1225")) and
      (__ \ 'airFuelRatioCmd).json.copyFrom(dr("kff124d"))) reduce

    val logToCoreData = (
      (__ \ 'id).json.copyFrom((__ \ "_id").json.pick[JsString]) and
      (__ \ 'eml).json.copyFrom((__ \ 'eml).json.pick) and
      (__ \ 'session).json.copyFrom((__ \ 'session).json.pick) and
      (__ \ "@timestamp").json.copyFrom(((__ \ 'time).read[String]).map(s => JsString(dateFormat.format(new Date(s.toLong)))))) reduce
    val q = Json.obj("indexed" -> false)
    //    SessionLogDao.findE(q)

    val result = for {
      data <- SessionLogDao.find(q)
    } yield {
      Logger.info(s"<-- got ${data.size} to perform index")

      data.foreach { js =>
        if (!(js \ "profileName").asOpt[JsValue].isDefined) {

          val js1 = js.transform(logToOBDDataJs).fold(invalid = { err => println(err + " js: " + js); Json.obj() }, valid = { js => js })
          val js2 = js.transform(logToCoreData).fold(invalid = { err => println(err + " js: " + js); Json.obj() }, valid = { js => js })

          val id = (js \ "_id" \ "$oid").asOpt[String] getOrElse { (js \ "_id").as[String] }

          val geoPoint1 = (js \ "kff1005").asOpt[String].map(_.toDouble)
          val geoPoint2 = (js \ "kff1006").asOpt[String].map(_.toDouble)

          var newJs = js1 ++ js2

          if (geoPoint1.isDefined && geoPoint2.isDefined)
            newJs = newJs ++ Json.obj("geoPoint" -> Json.arr(geoPoint1, geoPoint2))

          (js \ "sessionName").asOpt[String] map { v => newJs = newJs ++ Json.obj("sessionName" -> v) }

          esClient.index("obddata", "torquelogs", id, newJs)

          val q = Json.obj("_id" -> (js \ "_id"))
          SessionLogDao.update(q, Json.obj("indexed" -> true))
        }
      }

      Logger.info(s"index action completed -->")
      Ok("done")
    }
    result
  }

  val requiredField = (
    (__ \ 'v).json.pickBranch and
    (__ \ 'time).json.pickBranch and
    (__ \ 'session).json.pickBranch and
    (__ \ 'eml).json.pickBranch and
    (__ \ 'id).json.copyFrom((__ \ 'session).json.pick)) reduce

  val removeTime = (__ \ 'time).json.prune
  val removeId = (__ \ 'id).json.prune

  val removeRequiredField =
    (__ \ 'v).json.prune andThen
      removeTime andThen
      (__ \ 'session).json.prune andThen
      (__ \ 'eml).json.prune andThen
      removeId

  /** upload **/
  def upload = Action.async { request =>

    val flattenMap = request.queryString.map { qMap =>
      (qMap._1, qMap._2.head)
    }

    val js = Json.toJson(flattenMap).as[JsObject]
    log.debug(s"js received: $js")

    js.validate(requiredField) match {
      case s: JsSuccess[JsObject] =>
        val eml = (js \ "eml").as[String]
        UserProfile.getProfileByEmail(eml) flatMap { profile =>

          val mainResult = profile map { p =>

            val data = js.transform(removeRequiredField).get

            log.debug(s"data to persist: $data")

            if (data.toString.contains("default") || data.toString.contains("user") || data.toString.contains("profile")) {
              // header item

              SessionHeaderDao.find(s.get.transform(removeTime andThen removeId).get).map { l =>
                val fieldName = if (data.toString.contains("default")) "default" else if (data.toString.contains("default")) "user" else "profile"
                val updateData = Json.obj(fieldName -> data)

                if (l.size > 0) {
                  val objId = l.head \ "_id"
                  SessionHeaderDao.update(Json.obj("_id" -> objId), updateData)
                } else {
                  SessionHeaderDao.insert(s.get.transform(removeId).get ++ updateData)
                }

                Ok("OK!")
              }

            } else {
              if (data.toString.contains(":")) {
                // has data and
                val jsWithoutId = js.transform(removeId).get
                val result = SessionLogDao.insert(jsWithoutId ++ Json.obj("indexed" -> false))
                result map { le => Ok("OK!") }

              } else {
                // has no data
                Future.successful(BadRequest("ERR!"))
              }
            }

          } getOrElse {
            Future.successful(Unauthorized("ERR!"))
          }

          mainResult

        }
      case e: JsError =>
        Future.successful(BadRequest("ERR!"))
    }

    //println("-->")

  }

}