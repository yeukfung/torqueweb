package specs

import org.specs2.mutable.Specification
import models.UserProfile
import daos.UserProfileDao
import helpers.MyHelper._
import scala.concurrent.ExecutionContext.Implicits._
import play.api.test.WithApplication

class SampleDataSpec extends Specification with SpecUtil {

  "Sample data spec" should {

    "create 3 sample user" in new WithApplication {
      tryPerform(UserProfileDao.coll.drop)
      adminUtil.createUser("yeukfung@gmail.com")
      adminUtil.createUser("race@gmail.com", UserProfile.ROLE_race)
      adminUtil.createUser("admin@gmail.com", UserProfile.ROLE_admin)
    }
  }
  
}