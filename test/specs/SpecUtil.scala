package specs

import org.specs2.mutable.Specification
import daos.SessionHeaderDao
import daos.SessionLogDao
import daos.UserProfileDao
import play.api.test._
import play.api.test.Helpers._
import helpers.MyHelper._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json._
import play.api.libs.json.Reads._
import models.UserProfile

trait DefaultDur {
  implicit val dur: Duration = Duration(5, "seconds")
}

trait SpecUtil extends DefaultDur { this: Specification =>

  lazy val shDao = SessionHeaderDao
  lazy val slDao = SessionLogDao
  lazy val upDao = UserProfileDao

  def cleanDB = {
    val result = for {
      r1 <- tryPerform(shDao.coll.drop)
      r2 <- tryPerform(slDao.coll.drop)
      r3 <- tryPerform(upDao.coll.drop)
    } yield {
      r1 && r2 && r3
    }
    Await.result(result, dur)
  }

  /**
   * torque util
   */

  object torqueUtil {

    def upload(qstr: String, statusCode: Int = OK, contentStr: String = "OK!") = {
      val result = route(FakeRequest(GET, s"/torque?$qstr")).get
      status(result) must_== statusCode
      contentAsString(result) must_== contentStr
    }

  }

  /**
   * admin util
   */

  object adminUtil {

    val samplePass = "12345"
    implicit val userFormat = UserProfile.fmt

    def createUser(eml: String) = {
      val userProfile = UserProfile.createUserWithPass(eml, samplePass, "name of " + eml)
      val result = Await.result(upDao.insertT(userProfile) map (_.ok), dur)
      result must beTrue
      result
    }
    
  }

}

trait SessionLogGenerator {

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
    val params = Map(
      "v" -> 7,
      "eml" -> eml,
      "id" -> id,
      "time" -> System.currentTimeMillis(),
      "session" -> sessionId,
      "defaultUnit11" -> "",
      "defaultUnitff1222" -> "g",
      "defaultUnit0a" -> "kPa")
    mapToQueryString(params)
  }

}
