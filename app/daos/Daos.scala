package daos

import play.api.mvc.Controller
import play.api.libs.json.JsObject
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import models.UserProfile
import play.api.libs.json.Format
import play.api.libs.functional.syntax._
import play.api.libs.json._
import models.RaceCar
import models.Race
import reactivemongo.api._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController
import reactivemongo.core.commands.Command
import reactivemongo.core.commands.FindAndModify
import reactivemongo.core.commands.Update
import reactivemongo.bson.BSONDocument
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import helpers.Log
import reactivemongo.bson.BSONObjectID

trait BaseDao extends Controller with MongoController with Log {

  def dbName: String
  def coll: JSONCollection = db.collection[JSONCollection](dbName)

  def find(q: JsObject): Future[List[JsObject]] = {
    val cursor = coll.find(q).cursor[JsObject]
    cursor.collect[List]()
  }

  def findFirst(q: JsObject): Future[Option[JsObject]] = {
    val cursor = coll.find(q).cursor[JsObject]
    cursor.collect[List](1).map { _.headOption }
  }

  val hasId = (__ \ "_id").read[JsValue] or (__ \ "id").read[JsValue]
  val removeMongoId = (__ \ "_id").json.prune

  def insert(obj: JsObject) = {
    val bsonid = BSONObjectID.generate.stringify
    (obj.transform(hasId)).asOpt match {
      case None =>
        val objToSave = obj ++ Json.obj("_id" -> bsonid) ++ Json.obj("id" -> bsonid)
        coll.insert(objToSave) map { x => objToSave }

      case Some(jsid) =>
        this.update(Json.obj("_id" -> jsid), obj, multi = false) map { x => obj }
    }
  }

  def update(q: JsObject, upd: JsObject, multi: Boolean = false) = {
    val js = Json.obj("$set" -> upd.transform(removeMongoId).get)
    log.debug(s"BaseDao.update: js = $js and q = $q")
    coll.update(q, js, multi = multi)
  }

  def remove(q: JsObject, firstMatchOnly: Boolean = false) = coll.remove(q, firstMatchOnly = firstMatchOnly)
}

trait BaseTypedDao[T] extends BaseDao {
  def insertT(obj: T)(implicit fmt: Format[T]) = this.insert(Json.toJson(obj).as[JsObject])

  def updateT(q: JsObject, upd: T, multi: Boolean = false)(implicit fmt: Format[T]) = {
    val js = Json.obj("$set" -> fmt.writes(upd))
    coll.update(q, js, multi = multi)
  }

  def findFirstT(q: JsObject)(implicit fmt: Format[T]): Future[Option[T]] = {
    val cursor = coll.find(q).cursor[T]
    cursor.collect[List](1).map { _.headOption }
  }

  def findT(q: JsObject)(implicit fmt: Format[T]): Future[List[T]] = {
    val cursor = coll.find(q).cursor[T]
    cursor.collect[List]()
  }

}

object SessionHeaderDao extends BaseDao { val dbName = "sessionheaders" }

object SessionLogDao extends BaseDao { val dbName = "sessionlogs" }

object UserProfileDao extends BaseTypedDao[UserProfile] {
  val dbName = "userprofiles"

}

object RaceDao extends BaseTypedDao[Race] {
  val dbName = "races"
}

object RaceCarDao extends BaseTypedDao[RaceCar] {
  val dbName = "racecars"
}