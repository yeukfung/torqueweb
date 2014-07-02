package daos

import reactivemongo.core.commands.FindAndModify
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.Update
import helpers.Log
import scala.concurrent.Future
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import reactivemongo.api.indexes.NSIndex
import reactivemongo.api.indexes.Index
import reactivemongo.core.commands.Ascending
import reactivemongo.api.indexes.IndexType
import helpers.Cfg

object InitDBDao extends BaseDao with Log with Cfg {

  val dbName = "initdb"

  val prefixMongodbName = getString("mongodb.db", "torque") + "."

  val idx_id = Index(Seq(("id", IndexType.Ascending)))

  val indexes: List[NSIndex] = List(
    NSIndex(prefixMongodbName + UserProfileDao.dbName, idx_id),
    NSIndex(prefixMongodbName + RaceDao.dbName, idx_id),
    NSIndex(prefixMongodbName + RaceCarDao.dbName, idx_id))

  def ensureIndex = {
    indexes foreach { db.indexesManager.ensure(_) }
  }
}

object CounterDao extends BaseDao with Log {
  val dbName = "counters"

  /**
   *
   *             query: { _id: name },
   *             update: { $inc: { seq: 1 } },
   *             new: true
   *
   */

  val key_id = "id"
  def initId = initSeq(key_id, 0)
  def getNextId = getNextSeq(key_id)

  private def getNextSeq(name: String): Future[String] = {
    val q = BSONDocument("_id" -> name)
    val upd = Update(BSONDocument("$inc" -> BSONDocument("seq" -> 1)), true)
    val cmdFindAndModify = FindAndModify(dbName, query = q, modify = upd, upsert = false)
    db.command(cmdFindAndModify).map {
      case Some(doc) => doc.getAs[Double]("seq").map { d => d.toInt.toString } getOrElse {
        log.error(doc.toString)
        "-1"
      }
      case None =>
        val result = initSeq(name, 1) map { flag => "1" }
        Await.result(result, Duration(5, "seconds"))
    }
  }

  private def initSeq(name: String, seq: Int): Future[Boolean] = {
    val q = Json.obj("_id" -> name)
    this.findFirst(q) flatMap {
      case Some(found) =>
        coll.update(q, Json.obj("seq" -> seq)) map { _.ok }
      case None =>
        val seqObj = Json.obj("_id" -> name, "seq" -> seq)
        coll.insert(seqObj) map { _.ok }
    }
  }

}