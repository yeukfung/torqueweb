package specs.system

import org.specs2.mutable.Specification
import play.api.test.WithApplication
import specs.SpecUtil
import specs.SessionLogGenerator
import play.api.test._
import play.api.test.Helpers._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import models.UserProfile

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
  
  
  Given: the user does not have user account
  When: the guest try to access dashboard
  Then: server will redirect to /login page									
  When: the guest try to login
  Then: server will prompt invalid email									$e3
  
  
  Given: A race manager is in db
  When: the race manager access dashboard /race
  Then: the race manager will be redirected to /login page					
  When: the race manager enter the username and password
  Then: the trace manager will redirected to /race dashboard				$e4
  

  Given: A admin is in db
  When: the admin access dashboard /admin
  Then: the admin will be redirected to /login page					
  When: the admin enter the username and password
  Then: the trace manager will redirected to /admin dashboard				$e5

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

  def e3 = running(TestServer(3333), CHROME) { browser =>
    cleanDB

    val resp = route(FakeRequest(GET, "/")).get
    status(resp) must_== SEE_OTHER
    headers(resp) must haveValue("/login")

    browser.goTo("http://localhost:3333/login")
    browser.$("#errmsg").getTexts().size() must_== 0

    browser.$("#email").text("dummy1.com");
    browser.$("#password").text("dummy1.com");
    browser.$("#loginForm").submit()

    browser.url must equalTo("http://localhost:3333/login")
    browser.$("#errmsg").getTexts().get(0) must equalTo("invalid login")

  }

  def e4 = running(TestServer(3333), CHROME) { browser =>
    cleanDB

    adminUtil.createUser("valid@gmail.com", UserProfile.ROLE_race)

    val resp = route(FakeRequest(GET, "/race")).get
    status(resp) must_== SEE_OTHER
    headers(resp) must haveValue("/login")

    browser.goTo("http://localhost:3333/login")
    browser.$("#errmsg").getTexts().size() must_== 0

    browser.$("#email").text("valid@gmail.com");
    browser.$("#password").text("12345");
    browser.$("#loginForm").submit()

    browser.url must equalTo("http://localhost:3333/race")

  }

  def e5 = running(TestServer(3333), CHROME) { browser =>
    cleanDB

    adminUtil.createUser("valid@gmail.com", UserProfile.ROLE_admin)

    val resp = route(FakeRequest(GET, "/admin")).get
    status(resp) must_== SEE_OTHER
    headers(resp) must haveValue("/login")

    browser.goTo("http://localhost:3333/login")
    browser.$("#errmsg").getTexts().size() must_== 0

    browser.$("#email").text("valid@gmail.com");
    browser.$("#password").text("12345");
    browser.$("#loginForm").submit()

    browser.url must equalTo("http://localhost:3333/admin")

  }
}