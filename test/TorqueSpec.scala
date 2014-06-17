import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Before

@RunWith(classOf[JUnitRunner])
class TorqueUploadSpec extends Specification {

  def mapToString(m: Map[String, Any]) = m.map(item => s"${item._1}=${item._2}&").foldLeft("")((item, acc) => acc + item)

//  trait Env extends Before {
//    
//    def before = new WithApplication {
//      val content = route(FakeRequest(GET, s"/torque/pruneData/Dkd9ea29ud")).get
//      status(content) must_== OK
//    }
//  }

  "Torque upload header" should {

    "upload the header with required fields" in new WithApplication {
      val params = Map(
        "v" -> 7,
        "eml" -> "yeukfung",
        "id" -> "321",
        "time" -> 1402969539483L,
        "session" -> 1402969511745L,
        "defaultUnit11" -> "",
        "defaultUnitff1222" -> "g",
        "defaultUnit0a" -> "kPa")

      val qstr = mapToString(params)
      val result = route(FakeRequest(GET, s"/torque?$qstr")).get
      status(result) must_== OK
      contentAsString(result) must_== "OK!"
    }

    "upload the header userUnit with required field" in new WithApplication {
      val params = Map(
        "v" -> 7,
        "eml" -> "yeukfung",
        "id" -> "321",
        "time" -> 1402969539484L,
        "session" -> 1402969511745L,
        "userUnit11" -> "",
        "userUnitff1249" -> ":1",
        "userUnitff1221" -> "g")

      val qstr = mapToString(params)
      val result = route(FakeRequest(GET, s"/torque?$qstr")).get
      status(result) must_== OK
      contentAsString(result) must_== "OK!"
    }

    "receive 400 Bad request on invalid header field if no default units" in new WithApplication  {
      val params = Map(
        "v" -> 7,
        "eml" -> "yeukfung",
        "id" -> "321",
        "time" -> 1402969539483L,
        "session" -> 1402969511745L)

      val qstr = mapToString(params)
      val result = route(FakeRequest(GET, s"/torque?$qstr")).get
      status(result) must_== BAD_REQUEST
      contentAsString(result) must_== "ERR!"
    }

    "receive 400 Bad request on invalid header field if missing required fields" in new WithApplication  {
      val params = Map(
        "v" -> 7,
        "eml" -> "yeukfung",
        "time" -> 1402969539483L,
        "session" -> 1402969511745L,
        "defaultUnit11" -> "",
        "defaultUnitff1222" -> "g",
        "defaultUnit0a" -> "kPa")

      val qstr = mapToString(params)
      val result = route(FakeRequest(GET, s"/torque?$qstr")).get
      status(result) must_== BAD_REQUEST
      contentAsString(result) must_== "ERR!"
    }

  }

  "Torque upload session data" should {

    "upload the log with required fields" in new WithApplication {
      val params = Map(
        "v" -> 7,
        "eml" -> "yeukfung",
        "id" -> "321",
        "time" -> 1402969539483L,
        "session" -> 1402969511745L,
        "k11" -> 16.470589,
        "kff125d" -> 2.4863095,
        "kff1222" -> -0.7247865)

      val qstr = mapToString(params)
      val result = route(FakeRequest(GET, s"/torque?$qstr")).get
      status(result) must_== OK
      contentAsString(result) must_== "OK!"

    }

  }
}