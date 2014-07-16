package specs.admin

import specs.SpecUtil
import org.specs2.mutable.Specification
import models.UserProfile
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import helpers.SessionLogGenerator

class CreateLoginProfileSpec extends Specification with SpecUtil with SessionLogGenerator {
  override def is = s2"""
  
  Story: Admin Create Login Profile
  
  As an admin,
  I want to manage the Login user account 
  so that add/modify/disable/delete user account in the system
  
  Given admin is logined
  When admin create user with email user1@gmail.com
  Then user1@gmail.com is able to login, and upload data to system						$e1
  
  Given admin is logined and user1@gmail.com is valid
  When admin disable the user email user1@gmail.com
  Then user1@gmail.com is unable to login, and unable to upload data to system
  When admin delete the user email user1@gmail.com
  Then admin is no longer able to view the user1@gmail.com
  
  
"""

  def e1 = new WithApplication {
    cleanDB
    adminUtil.createUser("admin@gmail.com", UserProfile.ROLE_admin)
    val cookies = webUtil.login("admin@gmail.com", "12345")
    val json = Json.toJson(UserProfile.createUserWithPass("user1@gmail.com", "123456", "user1 name", UserProfile.ROLE_normal))
    val result = route(FakeRequest(POST, "/api/users").withJsonBody(json).withCookies(cookies.toList: _*)).get
    status(result) must_== 200
    (contentAsJson(result) \ "id").asOpt[String] must beSome

    val headerLog = genHeaderQueryString("user1@gmail.com")
    val sessionLog = genSessionLogQueryString(headerLog._1, "user1@gmail.com")

    torqueUtil.upload(headerLog._2, 200, "OK!")
    torqueUtil.upload(sessionLog, 200, "OK!")
  }
  
//  def e2 = new WithApplication {
//    e1
//    
//
//  }

}