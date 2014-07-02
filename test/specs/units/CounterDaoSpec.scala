package specs.units

import org.specs2.mutable.Specification
import daos.CounterDao
import scala.concurrent.Await
import specs.DefaultDur
import scala.concurrent.ExecutionContext.Implicits._
import play.api.test.WithApplication

class CounterDaoSpec extends Specification with DefaultDur {

  "Counter Dao Spec" should {

    "able to initSeq" in new WithApplication {
      val flag = CounterDao.initId
      Await.result(flag, dur) must beTrue
    }

    "able to getNextSequence in order" in new WithApplication {
      val flag = CounterDao.initId
      Await.result(flag, dur) must beTrue

      val result = for {
        id1 <- CounterDao.getNextId
        id2 <- CounterDao.getNextId
        id3 <- CounterDao.getNextId
      } yield {
        id1 + id2 + id3
      }

      Await.result(result, dur) must_== "123"
    }
    
    "able to do the upsert" in new WithApplication {
      Await.result(CounterDao.coll.drop, dur)

      val result = for {
        id1 <- CounterDao.getNextId
        id2 <- CounterDao.getNextId
      } yield {
        id1 + id2
      }

      Await.result(result, dur) must_== "12"
      
    }

  }
}