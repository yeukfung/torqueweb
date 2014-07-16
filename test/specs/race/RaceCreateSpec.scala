package specs.race

import org.specs2.mutable.Specification
import specs.SpecUtil
import models.UserProfile
import helpers.SessionLogGenerator
import play.api.test._
import play.api.test.Helpers._
import models.RaceCar
import play.api.libs.json.Json
import play.api.libs.json.JsArray
import models.ES
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Await
import controllers.Admin

class RaceCreateSpec extends Specification with SpecUtil with SessionLogGenerator {
  override def is = s2"""
  
  Story: Race Manager manage racing id spec
  
  As a race manager,
  I want to manage the race car 
  so that i can manage to see the racing information in real time info
  
  Given a race manager account is created
  When a invalid raceid upload to /race/upload
  Then ERR will returned
  When a race manager try to create a race car at /race/settings
  Then a race car upload id and will be generated
  When the race car upload to /race/upload
  Then a data is defined in elasticsearch /racing/<userid>										$e1
  

  
  Given a race manager account is created, and 3 race car has been created
  When a race manager rename the race car tag
  Then a race car name will be updated in the realtime dashboard
  
  Given a race manager account is created, and 3 race car has been created
  When a race manager disable the race
  Then the disabled car will be hidden in the race dashboard
  
  Given a race manager account is created, and 3 race car has been created
  When a race manager disable the race
  Then the disabled car will be hidden in the race dashboard
  
"""

  private def init() = {
    cleanDB
    
    Await.result(for {
      d <- ES.esClient.deleteIndex("racedata")
      m <- ES.esClient.createIndex("racedata", Some(Admin.jsonSetting))
    } yield {
      true
    }, dur) must beTrue

    adminUtil.createUser("racemgr@gmail.com", UserProfile.ROLE_race)
  }

  def e1 = new WithApplication {
    init()

    val raceId = "racemgr@gmail.com_1"
    val (sessId, param) = genHeaderQueryString(raceId)
    val sessLog1 = genSessionLogQueryString(sessId, raceId)

    val invalidResult1 = route(FakeRequest(GET, s"/upload/race?$param")).get
    val invalidResult2 = route(FakeRequest(GET, s"/upload/race?$sessLog1")).get

    status(invalidResult1) must_== OK
    status(invalidResult2) must_== BAD_REQUEST

    val loginCookie = webUtil.login("racemgr@gmail.com", "12345")
    loginCookie.size > 0 must beTrue

    // no car is fetched under this user account
    val noCarResult = route(FakeRequest(GET, s"/api/racecars").withCookies(loginCookie.toList: _*)).get

    status(noCarResult) must_== OK
    contentAsString(noCarResult) must_== "[]"

    // create race car via api
    val raceCarJs = Json.toJson(RaceCar("racemgr@gmail.com", "race car1"))

    val save = route(FakeRequest(POST, s"/api/racecars").withCookies(loginCookie.toList: _*).withJsonBody(raceCarJs)).get
    status(save) must_== OK

    val oneCarResult = route(FakeRequest(GET, s"/api/racecars").withCookies(loginCookie.toList: _*)).get
    status(oneCarResult) must_== OK

    contentAsString(oneCarResult) must contain("race car1")

    val rc_json_arr = contentAsJson(oneCarResult).as[JsArray]
    val rc_json = rc_json_arr(0)

    (rc_json \ "uploadId").asOpt[String] must beSome

    println(">>> " + rc_json)

    val uploadId = (rc_json \ "uploadId").as[String]

    val (sessIdValid, paramValid) = genHeaderQueryString(uploadId)
    val sessLogValid1 = genSessionLogQueryString(sessIdValid, uploadId)

    val validResult1 = route(FakeRequest(GET, s"/upload/race?$paramValid")).get
    val validResult2 = route(FakeRequest(GET, s"/upload/race?$sessLogValid1")).get

    status(validResult1) must_== OK
    status(validResult2) must_== OK

    // check data in elastic search after 2 seconds
    // should only has one data in the index  
    Thread.sleep(800)
    Await.result(ES.esClient.search("racedata", Json.obj()) map {
      resp =>
        resp.json.toString must contain(""""total":1""")

        true
    }, dur) must beTrue
  }

}