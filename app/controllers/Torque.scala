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
import models.RaceCar
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Torque extends Controller with MongoController with Log {

/**
47  Absolute Throttle Position B(%)
ff1223  Acceleration Sensor(Total)(g)
ff1220  Acceleration Sensor(X axis)(g)
ff1221  Acceleration Sensor(Y axis)(g)
ff1222  Acceleration Sensor(Z axis)(g)
49  Accelerator PedalPosition D(%)
4a  Accelerator PedalPosition E(%)
ff124d  Air Fuel Ratio(Commanded)(:1)
221564  AirCon High Side Pressure(bar)
46  Ambient air temp(Â°F)
ff1263  Average trip speed(whilst moving only)(km/h)
ff1272  Average trip speed(whilst stopped or moving)(km/h)
33  Barometric pressure (from vehicle)(kpa)
3c  Catalyst Temperature (Bank 1 Sensor 1)(Â°F)
44  Commanded Equivalence Ratio(lambda)
ff1258  CO2‚ in g/km (Average)(g/km)
ff1257  CO2‚ in g/km (Instantaneous)(g/km)
ff126a  Distance to empty (Estimated)(km)
31  Distance travelled since codes cleared(km)
5 Engine Coolant Temperature(Â°F)
ff1273  Engine kW (At the wheels)(kW)
4 Engine Load(%)
43  Engine Load(Absolute)(%)
0c  Engine RPM(rpm)
32  Evap System Vapour Pressure(Pa)
ff125c  Fuel cost (trip)(cost)
2f  Fuel Level (From Engine ECU)(%)
0a  Fuel pressure(kpa)
ff126b  Fuel Remaining (Calculated from vehicle profile)(%)
7 Fuel Trim Bank 1 Long Term(%)
14  Fuel trim bank 1 sensor 1(%)
6 Fuel Trim Bank 1 Short Term(%)
ff1271  Fuel used (trip)(l)
ff1239  GPS Accuracy(m)
ff1010  GPS Altitude(m)
ff123b  GPS Bearing(Â°)
ff1006  GPS Latitude(Â°)
ff1005  GPS Longitude(Â°)
ff123a  GPS Satellites
ff1237  GPS vs OBD Speed difference(km/h)
221145  H2OS Sensor(mV)
ff1226  Horsepower (At the wheels)(hp)
221141  Ignition 1 Voltage(V)
221538  Inlet air temp2 (IAT2)(Â°F)
0f  Intake Air Temperature(Â°F)
0b  Intake Manifold Pressure(kpa)
ff1203  Kilometers Per Litre(Instant)(kpl)
ff5202  Kilometers Per Litre(Long Term Average)(kpl)
2211a6  Knock Retard(Deg.)
ff1207  Litres Per 100 Kilometer(Instant)(l/100km)
ff5203  Litres Per 100 Kilometer(Long Term Average)(l/100km)
10  Mass Air Flow Rate(g/s)
ff1201  Miles Per Gallon(Instant)(mpg)
ff5201  Miles Per Gallon(Long Term Average)(mpg)
ff1214  O2 Volts Bank 1 sensor 1(V)
ff1215  O2 Volts Bank 1 sensor 2(V)
221154  Oil Temperature (Engine)(Â°F)
221161  Outside air temperature(Â°F)
45  Relative Throttle Position(%)
1f  Run time since engine start(s)
ff1001  Speed (GPS)(km/h)
0d  Speed (OBD)(km/h)
11  Throttle Position(Manifold)(%)
0e  Timing Advance(Â°)
ff1225  Torque(ft-lb)
221940  Transmission Fluid Temp (GM Method 1)(Â°F)
221940  Transmission Fluid Temp (GM Method 2)(Â°F)
ff120c  Trip distance (stored in vehicle profile)(km)
ff1266  Trip Time(Since journey start)(s)
ff1268  Trip time(whilst moving)(s)
ff1267  Trip time(whilst stationary)(s)
ff1202  Turbo Boost & Vacuum Gauge(bar)
42  Voltage (Control Module)(V)
ff1238  Voltage (OBD Adapter)(V)
ff1269  Volumetric Efficiency (Calculated)(%)
**/
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

  val TZ8HR = (8 * 1000 * 60 * 60)
  val logToCoreData = (
    (__ \ 'id).json.copyFrom((__ \ "_id").json.pick[JsString] or (__ \ "time").json.pick[JsString]) and
    (__ \ 'eml).json.copyFrom((__ \ 'eml).json.pick) and
    (__ \ 'session).json.copyFrom((__ \ 'session).json.pick) and
    (__ \ "@timestamp").json.copyFrom(((__ \ 'time).read[String]).map(s => JsString(dateFormat.format(new Date(s.toLong - TZ8HR))))) and
    (__ \ "utctime").json.copyFrom(((__ \ 'time).read[String]).map(s => JsString(dateFormat.format(new Date(s.toLong - TZ8HR)))))) reduce

  private def doConvertToElasticSearch(js: JsObject): (String, JsObject) = {
    val js1 = js.transform(logToOBDDataJs).fold(invalid = { err => println("logToOBDDataJs err: " + err + " js: " + js); Json.obj() }, valid = { js => js })
    val js2 = js.transform(logToCoreData).fold(invalid = { err => println("logToCoreData err: " + err + " js: " + js); Json.obj() }, valid = { js => js })

    val idRead = ((__ \ "_id" \ "$oid").read[String] or (__ \ "_id").read[String] or (__ \ "id").read[String])

    val id = js.validate(idRead).getOrElse("NA")

    val geoPoint1 = (js \ "kff1005").asOpt[String].map(_.toDouble)
    val geoPoint2 = (js \ "kff1006").asOpt[String].map(_.toDouble)

    var newJs = js1 ++ js2

    if (geoPoint1.isDefined && geoPoint2.isDefined)
      newJs = newJs ++ Json.obj("geoPoint" -> Json.arr(geoPoint1, geoPoint2))

    (js \ "sessionName").asOpt[String] map { v => newJs = newJs ++ Json.obj("sessionName" -> v) }
    (id, performAnalysis(newJs))
  }

  def submitToElasticSearch = Action.async {

    val q = Json.obj("indexed" -> false)
    //    SessionLogDao.findE(q)

    val result = for {
      data <- SessionLogDao.find(q)
    } yield {
      Logger.info(s"<-- got ${data.size} to perform index")

      data.foreach { js =>

        if (!(js \ "profileName").asOpt[JsValue].isDefined) {

          val (id, newJs) = doConvertToElasticSearch(js)
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

  def uploadRace = Action.async { request =>
    val flattenMap = request.queryString.map { qMap =>
      (qMap._1, qMap._2.head)
    }

    val js = Json.toJson(flattenMap).as[JsObject]
    log.debug(s"racing js received: $js")

    val newJs = if ((js \ "eml").asOpt[String].isDefined) {
      js
    } else {
      val deviceId = (js \ "id").as[String]
      Await.result(UserProfile.getProfileByDeviceId(deviceId) map {
        case Some(p) => js ++ Json.obj("eml" -> p.eml)
        case None => js
      }, Duration(2, "seconds"))
    }

    newJs.validate(requiredField) match {
      case s: JsSuccess[JsObject] =>
        // discard header

        if (js.toString.contains("default") || js.toString.contains("user") || js.toString.contains("profile")) {
          log.debug(s"skipped this js as it's header content: $js")
          Future.successful(Ok("OK!"))
        } else {
          val eml = (js \ "eml").as[String]

          RaceCar.findByUploadId(eml) flatMap {
            case Some(rc) =>

              // has data and
              val genId = BSONObjectID.generate.stringify
              val (id, newJs) = doConvertToElasticSearch(js)
              esClient.index("racedata", "torquelogs", genId, newJs) map { a => Ok("OK!") }

            case None =>
              log.debug(s"not found the uploadId: $eml")
              Future.successful(BadRequest("ERR!"))

          }
        }
      case e: JsError =>
        Future.successful(BadRequest("Invalid Torque Data"))

    }

  }

  private def processJs(js: JsObject) = {
    js.validate(requiredField) match {
      case s: JsSuccess[JsObject] =>
        val eml = (js \ "eml").as[String]
        UserProfile.getProfileByEmail(eml) flatMap { profile =>

          val mainResult = profile map { p =>

            val data = js.transform(removeRequiredField).get

            log.debug(s"data to persist: $data")

            // special handle for the torque free version
            if ((js \ "source").asOpt[String].isDefined && (js \ "source").as[String] == "torquefree") {
              // add header if not found
              SessionHeaderDao.find(s.get.transform(removeTime andThen removeId).get).map { l =>
                if (l.size == 0) {
                  SessionHeaderDao.insert(s.get.transform(removeId).get)
                }
              }
            }

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
  }
  /** upload **/
  def upload = Action.async { request =>

    val flattenMap = request.queryString.map { qMap =>
      (qMap._1, qMap._2.head)
    }

    val js = Json.toJson(flattenMap).as[JsObject]
    //    log.debug(s"js received: $js")

    val newJs = if ((js \ "eml").asOpt[String].isDefined) {
      js
    } else {
      val deviceId = (js \ "id").as[String]
      Await.result(UserProfile.getProfileByDeviceId(deviceId) map {
        case Some(p) =>
          log.debug(s"uploading using torque free and found ID $deviceId")
          js ++ Json.obj("eml" -> p.eml, "source" -> "torquefree")
        case None =>
          log.debug(s"uploading using torque free and UNABLE to find the deviceId in user profile: $deviceId")
          js
      }, Duration(2, "seconds"))
    }

    processJs(newJs)

    //println("-->")

  }

  //  def uploadFree = Action.async {
  //    request =>
  //      val flattenMap = request.queryString.map { qMap =>
  //        (qMap._1, qMap._2.head)
  //      }
  //      val js = Json.toJson(flattenMap).as[JsObject]
  //      val deviceId = (js \ "id").as[String]
  //      UserProfile.getProfileByDeviceId(deviceId) flatMap {
  //        case Some(profile) =>
  //          log.debug(s"re-mapping the encoded device id $deviceId for user email ${profile.eml} ")
  //          processJs(js ++ Json.obj("eml" -> profile.eml))
  //        case None =>
  //          log.debug("unknown free torque data is submitted from encoded device id: " + deviceId)
  //          Future.successful(Ok("OK!"))
  //      }
  //  }

  def performAnalysis(js: JsObject): JsObject = {
    var jsObj = js
    val speedTag = (js \ "speed").asOpt[Int] match {
      case Some(s) if s == 0 => Json.obj("tag_speed" -> "STOP")
      case Some(s) if s > 0 && s <= 40 => Json.obj("tag_speed" -> "SLOW")
      case Some(s) if s > 40 && s <= 70 => Json.obj("tag_speed" -> "MID")
      case Some(s) if s > 70 => Json.obj("tag_speed" -> "HIGH")
      case None => Json.obj()
    }

    jsObj ++ speedTag
  }

}