package helpers

import play.api.libs.json.Json
import scala.util.Random
import akka.actor.Actor
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Cancellable
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import controllers.Torque

class GenSessionLogActor extends Actor with Log with SessionLogGenerator {

  var cancelMap: Map[String, (Long, Cancellable)] = Map.empty

  def receive = {
    case ("gen", raceId: String) =>
      val HOST = "http://localhost:9000"

      log.debug("generate with id: " + raceId)

      cancelMap.get(raceId) match {
        case Some(x) =>
          val (sessionId, _) = x
          val sessionLogParam = genSessionLogQueryString(sessionId, raceId)
          WS.url(s"$HOST/upload/race?$sessionLogParam").get().map {
            resp =>
              log.debug(s"got response from svr: ${resp.body.toString()}")
          }

          Akka.system.scheduler.scheduleOnce(Duration(1, "second"), self, ("gen", raceId))
        case None =>
          log.debug(s"unable to locate raceId: $raceId")
      }

    case ("start", raceId: String) =>
      val cancelHook = Akka.system.scheduler.scheduleOnce(Duration(1, "second"), self, ("gen", raceId))
      if (!cancelMap.get(raceId).isDefined) {
        val (sessionId, param) = genHeaderQueryString(raceId)
        cancelMap += (raceId -> (sessionId, cancelHook))
        log.debug(s"started with sessionId: $sessionId")

        //cancelHook.cancel()
      }

    case ("stop", raceId: String) =>
      if (cancelMap.get(raceId).isDefined) {
        val cancelled = cancelMap(raceId)._2.cancel()
        cancelMap -= raceId
        log.debug(s"stopped with cancelled flag: $cancelled")
      }

  }

  override def preRestart(ex: Throwable, opt: Option[Any]) {
    log.error(ex.getMessage())
    cancelMap.foreach(f => f._2._2.cancel)
  }
}

trait SessionLogGenerator {
  import helpers.MyHelper._

  def genHeaderQueryString(eml: String = "yeukfung@gmail.com", id: String = "12345") = {

    val sessionId = (System.currentTimeMillis() + 1L)

    val params = Map(
      "v" -> 7,
      "eml" -> eml,
      "id" -> id,
      "time" -> System.currentTimeMillis(),
      "session" -> sessionId,
      "defaultUnit11" -> "",
      "defaultUnitff1222" -> "g",
      "defaultUnit0a" -> "kPa")

    (sessionId, mapToQueryString(params))
  }

  def genSessionLogQueryString(sessionId: Long, eml: String = "yeukfung@gmail.com", id: String = "12345") = {

    //      (__ \ 'speed).json.copyFrom(dr("kd")) and 
    //      (__ \ 'engineLoad).json.copyFrom(dr("k4")) and
    //      (__ \ 'engineRPM).json.copyFrom(dr("kc")) and
    //      (__ \ 'throttlePos).json.copyFrom(dr("k11")) and
    //      (__ \ 'kpl).json.copyFrom(dr("kff1203")) and
    //      (__ \ 'airFuelRatio).json.copyFrom(dr("kff1249")) and
    //      (__ \ 'intakeAirTemp).json.copyFrom(dr("kf")) and
    //      (__ \ 'intakeManifoldPressure).json.copyFrom(dr("kb")) and
    //      (__ \ 'engineCoolantTemp).json.copyFrom(dr("k5")) and
    //      (__ \ 'co2InGperKM).json.copyFrom(dr("kff1257")) and
    //      (__ \ 'fuelFlowRate).json.copyFrom(dr("kff125d")) and
    //      (__ \ 'fuelPressure).json.copyFrom(dr("ka")) and
    //      (__ \ 'massAirFlowRate).json.copyFrom(dr("k10")) and
    //      (__ \ 'timingAdvance).json.copyFrom(dr("ke")) and
    //      (__ \ 'vacuum).json.copyFrom(dr("kff1202")) and
    //      (__ \ 'voltage).json.copyFrom(dr("kff1238")) and
    //      (__ \ 'barometer).json.copyFrom(dr("kff1270")) and
    //      (__ \ 'torque).json.copyFrom(dr("kff1225")) and
    //      (__ \ 'airFuelRatioCmd).json.copyFrom(dr("kff124d"))) reduce

    val params = Map(
      "v" -> 7,
      "eml" -> eml,
      "id" -> id,
      "time" -> System.currentTimeMillis(),
      "session" -> sessionId,
      "kd" -> Math.abs(Random.nextInt(200)),
      "k4" -> Math.abs(Random.nextDouble() * 100),
      "kc" -> Math.abs(Random.nextInt(6000) + 700),
      "k11" -> Math.abs(Random.nextDouble() * 80))

    mapToQueryString(params)
  }

}
