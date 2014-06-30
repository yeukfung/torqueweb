package specs.system

import org.specs2.mutable.Specification
import play.api.test.WithApplication
import specs.SpecUtil
import specs.SessionLogGenerator
import play.api.test._
import play.api.test.Helpers._

class SystemSecuritySpec extends Specification with SpecUtil with SessionLogGenerator {

  override def is = s2"""
  Story: System Security Spec
  
  As a system security module
  I want to make sure only authenticated user are allowed to upload
  so that I can make sure each user with different role have access to certain functions
  
  
  Given: a clean db
  When: guest try to upload the data
  Then server will response 401 Unauthorized								$e1
  

  Given: clean db and a valid user: valid@gmail.com created by admin
  When: A valid user try to upload the data
  Then server will response 200 and OK!										$e2
  
  
  Given: A guest want to access dashboard
  When: the guest access dashboard
  Then: server will redirect to /login page
  
  
  Given: A race manager is access dashboard
  When: the race manager enter invalid login
  Then: invalid login will be displayed

  """

  def e1 = new WithApplication {
    cleanDB

    val dummyHeader = genHeaderQueryString("dummy@dummy.com")
    val sessionId = dummyHeader._1
    val dummyLog = genSessionLogQueryString(sessionId, "dummy@dummy.com")

    torqueUtil.upload(dummyHeader._2, UNAUTHORIZED, "ERR!")
    torqueUtil.upload(dummyLog, UNAUTHORIZED, "ERR!")

  }

  def e2 = new WithApplication {
    cleanDB

    adminUtil.createUser("valid@gmail.com")

    val (sessionId, validHeaderLog) = genHeaderQueryString("valid@gmail.com")
    val validSessionLog = genSessionLogQueryString(sessionId, "valid@gmail.com")

    torqueUtil.upload(validHeaderLog, OK, "OK!")
    torqueUtil.upload(validSessionLog, OK, "OK!")

  }

}