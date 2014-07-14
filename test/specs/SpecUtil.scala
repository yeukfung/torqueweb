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
import org.openqa.selenium.chrome.ChromeDriver
import daos.RaceCarDao
import helpers.DefaultDur
import play.api.mvc.Cookie
import play.api.mvc.Cookies

trait ChromeWebDriver {
  System.setProperty("webdriver.chrome.driver", "driver/mac/chromedriver");
  val CHROME = classOf[ChromeDriver]
}

trait SpecUtil extends DefaultDur with ChromeWebDriver { this: Specification =>

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
   * race util
   */
  object raceUtil {

    def createRaceCar(eml: String, js: JsObject): JsObject = {
      val obj = js ++ Json.obj("parent" -> eml)
      val r = RaceCarDao.insert(obj).map { _ => obj }
      Await.result(r, dur)
    }
  }

  /**
   * admin util
   */

  object adminUtil {

    val samplePass = "12345"
    implicit val userFormat = UserProfile.fmt

    def createUser(eml: String, role: String = UserProfile.ROLE_normal) = {
      val userProfile = UserProfile.createUserWithPass(eml, samplePass, "name of " + eml, role = role)
      val result = Await.result(upDao.insertT(userProfile) map { js => (js \ "id").asOpt[JsValue].isDefined }, dur)
      result must beTrue
      result
    }

  }

  /**
   * web util
   */
  object webUtil {

    val loginPath = "/login"
    def login(eml: String, pass: String): Cookies = {
      val result = route(FakeRequest(POST, loginPath).withFormUrlEncodedBody(("email" -> eml), ("password" -> pass))).get
      status(result) must_== SEE_OTHER
      session(result).get("email") must beSome
      cookies(result)
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
