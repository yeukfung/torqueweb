package models

import scala.concurrent.Future
import daos.RaceCarDao
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits._
import daos.CounterDao
import helpers.Log

case class Race(parent: String, name: String, location: String, tag: String, startTime: Long, id: Option[String] = None)

object RaceCar extends Log {

  implicit val fmt = Json.format[RaceCar]

  def saveRaceCar(userId: String, car: RaceCar): Future[RaceCar] = {

    car.id match {
      case Some(id) =>
        val q = Json.obj("id" -> id)
        val updatedCar = car.copy(userId = Some(userId))
        log.debug(s"updating car: $updatedCar")

        RaceCarDao.updateT(q, updatedCar).map { le => updatedCar }

      case None =>
        CounterDao.getNextId flatMap { idx =>

          val updatedCar0 = car.copy(idx = Some(idx), userId = Some(userId))
          val updatedCar = updatedCar0.copy(uploadId = Some(updatedCar0.genUploadId))
          log.debug(s"inserting car: $updatedCar")
          RaceCarDao.insertT(updatedCar).map { le => updatedCar }
        }
    }
  }

  def findByUploadId(uploadId: String): Future[Option[RaceCar]] = {
    val q = Json.obj("uploadId" -> uploadId)
    log.debug(s"findByUploadId: $q")
    RaceCarDao.findFirstT(q)
  }

  val DRIVE_FF = "ff"
  val DRIVE_FR = "fr"
  val DRIVE_4WD = "4wd"
  val DRIVE_MR = "mr"

}

case class RaceCar(
  eml: String,
  name: String,
  carMake: Option[String] = None,
  carModel: Option[String] = None,
  engineCC: Option[String] = None,
  carWeight: Option[String] = None,
  drive: Option[String] = None, // ff/fr/4wd/mr
  carClass: Option[String] = None,
  userId: Option[String] = Some("NA"),
  uploadId: Option[String] = Some("NA"), // system field, will be override
  idx: Option[String] = Some("-1"),
  deviceId: Option[String] = None,
  deviceIdEncoded: Option[String] = None,
  id: Option[String] = None) {
  def genUploadId = s"${eml}_${idx.get}"
}