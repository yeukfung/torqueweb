package controllers

import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json.Json
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits._

object Torque extends Controller with MongoController {

  def collHeaders: JSONCollection = db.collection[JSONCollection]("sessionheaders")
  def collSessionLogs: JSONCollection = db.collection[JSONCollection]("sessionlogs")

  private def tryPerform(f: () => Future[Boolean]): Future[Boolean] = {
    try {
      f().fallbackTo(Future(false))
    } catch {
      case _: Throwable => Future(false)
    }
  }

  def pruneData(apiKey: String) = Action.async { request =>

    if (apiKey == "Dkd9ea29ud") {
      for {
        result1 <- tryPerform(collHeaders.drop)
        result2 <- tryPerform(collSessionLogs.drop)
      } yield {
        Ok
      }
    } else Future.successful(Forbidden)

  }

  val pidMap = Map(
    "04" -> ("Engine Load", "%"),
    "0c" -> ("Engine RPM", "rpm"),
    "11" -> ("Throttle Position(Manifold)", "%"),
    "ff1203" -> ("Kilometers Per Litre(Instant)", "kpl"),
    "0d" -> ("Speed", "km/h"),
    "ff1249" -> ("Air Fuel Ratio(Measured)", "1"),
    "0f" -> ("Intake Air Temperature", "°C"),
    "0b" -> ("Intake Manifold Pressure", "kPa"),

    "ff1258" -> ("CO2 in g/km (Average)", "g/km"),
    "05" -> ("Engine Coolant Temperature", "°C"),
    "ff1257" -> ("CO2 in g/km (Instantaneous)", "g/km"),
    "ff125d" -> ("Fuel flow rate/hour", "l/hr"),
    "2f" -> ("Fuel Level", "%"),
    "0a" -> ("Fuel Pressure", "kPa"),
    "10" -> ("Mass Air Flow Rate", "g/s"),
    "0e" -> ("Timing Advance", "°"),
    "ff1202" -> ("Turbo Boost & Vacuum Gauge", "psi"),
    "ff1238" -> ("Voltage (OBD Adapter)", "V"),
    "ff1273" -> ("Engine kW(At the wheels)", "kW"),
    "ff1270" -> ("Barometer (on Android device)", "mb"),
    "ff1225" -> ("Torque", "ft-lb"),

    "ff124d" -> ("Air Fuel Ratio(Commanded)", "1"))

  import play.api.libs.json._ // JSON library
  import play.api.libs.json.Reads._ // Custom validation helpers
  import play.api.libs.functional.syntax._ // Combinator syntax

  val requiredField = (
    (__ \ 'v).json.pickBranch and
    (__ \ 'time).json.pickBranch and
    (__ \ 'session).json.pickBranch and
    (__ \ 'eml).json.pickBranch and
    (__ \ 'id).json.pickBranch) reduce

  val removeTime = (__ \ 'time).json.prune

  val removeRequiredField =
    (__ \ 'v).json.prune andThen
      removeTime andThen
      (__ \ 'session).json.prune andThen
      (__ \ 'eml).json.prune andThen
      (__ \ 'id).json.prune

  def upload = Action.async { request =>
    println(request)
    val flattenMap = request.queryString.map { qMap =>
      (qMap._1, qMap._2.head)
    }

    val js = Json.toJson(flattenMap)

    js.validate(requiredField) match {
      case s: JsSuccess[JsObject] =>
        val data = js.transform(removeRequiredField).get

        if (data.toString.contains("default") || data.toString.contains("user")) {
          // header item

          val cursor = collHeaders.find(s.get.transform(removeTime).get).cursor[JsObject]
          cursor.collect[List]().map { l =>
            val fieldName = if (data.toString.contains("default")) "default" else "user"
            val updateData = Json.obj(fieldName -> data)

            if (l.size > 0) {
              val objId = l.head \ "_id"
              val updateCmd = Json.obj("$set" -> updateData)
              collHeaders.update(Json.obj("_id" -> objId), updateCmd)
            } else {
              collHeaders.insert(s.get ++ updateData)
            }

            Ok("OK!")
          }

        } else {
          if (data.toString.contains(":")) {
            // has data and
            val result = collSessionLogs.insert(js)
            result map { le =>
              Ok("OK!")
            }
          } else {
            // has no data

            Future.successful(BadRequest("ERR!"))
          }
        }
      case e: JsError =>
        Future.successful(BadRequest("ERR!"))
    }

    //println("-->")

  }
}