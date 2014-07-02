import play.api._
import helpers.Log
import daos.InitDBDao

object Global extends GlobalSettings with Log {

  override def onStart(app: Application) {
    log.info("Application has started")

    log.debug("loading mongodb ensure index")
    InitDBDao.ensureIndex
  }

}