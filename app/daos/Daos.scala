package daos

import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController
import play.api.mvc.Controller
import play.api.libs.json.JsObject
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import models.UserProfile
import play.api.libs.json.Format
import play.api.libs.json.Json

trait BaseDao extends Controller with MongoController {

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

  def insert(obj: JsObject) = coll.insert(obj)

  def update(q: JsObject, upd: JsObject, multi: Boolean = false) = coll.update(q, upd, multi = multi)

  def remove(q: JsObject, firstMatchOnly: Boolean = false) = coll.remove(q, firstMatchOnly = firstMatchOnly)
}

trait BaseTypedDao[T] extends BaseDao {
  def insertT(obj: T)(implicit fmt: Format[T]) = coll.insert(obj)

  def updateT(q: JsObject, upd: T, multi: Boolean = false)(implicit fmt: Format[T]) = {
    val js = Json.obj("$set" -> fmt.writes(upd))
    coll.update(q, js, multi = multi)
  }

  def findFirstT(q: JsObject)(implicit fmt: Format[T]): Future[Option[T]] = {
    val cursor = coll.find(q).cursor[T]
    cursor.collect[List](1).map { _.headOption }
  }

}

object SessionHeaderDao extends BaseDao { val dbName = "sessionheaders" }

object SessionLogDao extends BaseDao { val dbName = "sessionlogs" }

object UserProfileDao extends BaseTypedDao[UserProfile] {
  val dbName = "userprofiles"

}