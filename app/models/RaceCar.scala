package models

import scala.concurrent.Future
import daos.RaceCarDao
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits._

case class Race(parent: String, name: String, location: String, tag: String, startTime: Long, id: Option[String] = None)

object RaceCar {

  implicit val fmt = Json.format[RaceCar]
  
  def saveRaceCar(car: RaceCar): Future[RaceCar] = {
    val uploadId = car.genUploadId;
    val updatedCar = car.copy(uploadId = uploadId)
    
    updatedCar.id match {
      case Some(id) =>
        val q = Json.obj("id" -> id)
        RaceCarDao.updateT(q, updatedCar).map {le => updatedCar}
      case None =>
        RaceCarDao.insertT(updatedCar.copy(id = Some("getNextSeq('key')"))).map {le => updatedCar} 
    }
  }

  val DRIVE_FF = "ff"
  val DRIVE_FR = "fr"
  val DRIVE_4WD = "4wd"
  val DRIVE_MR = "mr"

}

case class RaceCar(parent: String,
  raceId: Set[String] = Set.empty,
  idx: Int,
  name: String,
  uploadId: String, // system field, will be override
  carMake: Option[String] = None,
  carModel: Option[String] = None,
  engineCC: Option[String] = None,
  carWeight: Option[String] = None,
  drive: Option[String] = None, // ff/fr/4wd/mr
  carClass: Option[String] = None,
  id: Option[String] = None) {
  def genUploadId = s"${parent}_${idx}"
}