package specs.race

import org.specs2.mutable.Specification
import specs.SpecUtil
import play.api.test.WithApplication
import models.UserProfile
import play.api.libs.json.Json

class RaceUploadSpec extends Specification with SpecUtil {

  override def is = s2"""

  Story: Racecar upload Data
  
  Given a race car is ready
  When it uploads data to /torque
  Then ERR! will received
  
  Given a race car is ready
  When it upload data to /race/torque
  Then OK! will received
  
  Given a race manager account is created, and 3 race car has been created
  When 2 race car upload the log data
  Then the dashboard should reflex the changes within 5 seconds

  """

  def e1 = new WithApplication {
    adminUtil.createUser("race@gmail.com", UserProfile.ROLE_race) must beTrue

    //    val js = Json.parse("""{
    //      
    //    }""").as[JsObject]

    //    raceUtil.createRaceCar("race@gmail.com", js)
  }
}