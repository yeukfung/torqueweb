package specs.user

import org.specs2.mutable.Specification
import play.api.test.WithApplication
import play.api.test._
import play.api.test.Helpers._
import helpers.MyHelper._
import specs.SpecUtil
import daos.SessionHeaderDao
import play.api.libs.json.Json
import scala.concurrent.Await
import daos.SessionLogDao

class UserTorqueUploadSpec extends Specification with SpecUtil {

  override def is = s2"""
  
  Story: User Uploading log via Torque
  
  As a User
  I want to upload obddata log to webserver
  so that i have the analytic data of my car
  
  Given: torque uploader with valid header info
  When: user upload the data to webserver
  Then: server will response OK!
  Then: server will only has one session header record			$e1
  
  Given: torque uploader sending invalid item
  When: server receive request
  Then: server will response ERR!								$e2
  
  Given: torque uploader is sending sessionData
  When: server receive sessionlog data
  Then: server will save the data to sessionlogs collection		
  Then: session log is indexed = false
  When: server submit the index to elastic search
  Then: session log is become indexed							$e3
  
  """

  def init() {
    cleanDB
    adminUtil.createUser("yeukfung")
  }

  def e1 = new WithApplication {
    init()
    val params = Map(
      "v" -> 7,
      "eml" -> "yeukfung",
      "id" -> "321",
      "time" -> 1402969539483L,
      "session" -> 1402969511745L,
      "defaultUnit11" -> "",
      "defaultUnitff1222" -> "g",
      "defaultUnit0a" -> "kPa")

    val qstr = mapToQueryString(params)
    val result = route(FakeRequest(GET, s"/torque?$qstr")).get
    status(result) must_== OK
    contentAsString(result) must_== "OK!"

    val params1 = Map(
      "v" -> 7,
      "eml" -> "yeukfung",
      "id" -> "321",
      "time" -> 1402969539484L,
      "session" -> 1402969511745L,
      "userUnit11" -> "",
      "userUnitff1249" -> ":1",
      "userUnitff1221" -> "g")

    val qstr1 = mapToQueryString(params1)
    val result1 = route(FakeRequest(GET, s"/torque?$qstr1")).get
    status(result1) must_== OK
    contentAsString(result1) must_== "OK!"
      
    val list = Await.result(SessionHeaderDao.find(Json.obj()), dur)
    list.size must_== 1

  }

  def e2 = new WithApplication {
    init()
    val params = Map(
      "v" -> 7,
      "eml" -> "yeukfung",
      "id" -> "321",
      "time" -> 1402969539483L,
      "session" -> 1402969511745L)

    val qstr = mapToQueryString(params)
    val result = route(FakeRequest(GET, s"/torque?$qstr")).get
    status(result) must_== BAD_REQUEST
    contentAsString(result) must_== "ERR!"

    val params1 = Map(
      "v" -> 7,
      "eml" -> "yeukfung",
      "time" -> 1402969539483L,
      "defaultUnit11" -> "",
      "defaultUnitff1222" -> "g",
      "defaultUnit0a" -> "kPa")

    val qstr1 = mapToQueryString(params1)
    val result1 = route(FakeRequest(GET, s"/torque?$qstr1")).get
    status(result1) must_== BAD_REQUEST
    contentAsString(result1) must_== "ERR!"

  }

  def e3 = new WithApplication {
    init()
    val params = Map(
      "v" -> 7,
      "eml" -> "yeukfung",
      "id" -> "321",
      "time" -> 1402969539483L,
      "session" -> 1402969511745L,
      "k11" -> 16.470589,
      "kff125d" -> 2.4863095,
      "kff1222" -> -0.7247865)

    val qstr = mapToQueryString(params)
    val result = route(FakeRequest(GET, s"/torque?$qstr")).get
    status(result) must_== OK
    contentAsString(result) must_== "OK!"

      
    val list = Await.result(SessionLogDao.find(Json.obj()), dur)
    list.size must_== 1
      
    
    (list.head \ "indexed").as[Boolean] must_== false
    
    val es = route(FakeRequest(GET, "/torque/sendToES")).get
    status(es) must_== OK
    contentAsString(es) must_== "done"
      
    val listAfter = Await.result(SessionLogDao.find(Json.obj()), dur)
    listAfter.size must_== 1
    
    (listAfter.head \ "indexed").as[Boolean] must_== true
    
  }

}