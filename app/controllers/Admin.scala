package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.libs.json._
import helpers.MyHelper._
import models.ES
import daos.SessionLogDao
import play.api.templates.Html
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import daos.SessionHeaderDao
import models.UserProfile

object Admin extends Controller with MySecured {

  def index = Authenticated(role = Some(UserProfile.ROLE_admin)).async { implicit request =>
    Future.successful(Ok(views.html.admin.index()))
  }

  val jsonSetting = Json.parse("""
		  {
		  "torquelogs" : {
		  "properties" : {
		  "sessionName" : {
		  	"type" : "string", "index" : "not_analyzed"
		  },
		  "session" : {
		  	"type" : "string", "index" : "not_analyzed"
		  },
		  "eml" : {
		  	"type" : "string", "index" : "not_analyzed"
		  }
		  }
		  }
		  }
		  
		  """).as[JsObject]

  /** admin db **/
  def resetAllLogAndIndex(apiKey: String) = Authenticated(role = Some(UserProfile.ROLE_admin)).async { request =>
    if ("kdkfei12123dkei4p" == apiKey) {

      val q = Json.obj()
      // reset 
      for {
        deleteOk <- ES.esClient.deleteIndex("obddata")
        createOk <- ES.esClient.createIndex("obddata", Some(jsonSetting))
        delete1Ok <- ES.esClient.deleteIndex("racedata")
        create1Ok <- ES.esClient.createIndex("racedata", Some(jsonSetting))
        mappingOk <- ES.esClient.index("obddata", "torquelogs", "_mapping", jsonSetting)
        mapping1Ok <- ES.esClient.index("racedata", "torquelogs", "_mapping", jsonSetting)
        //        cntLog <- SessionLogDao.find(q)
        updateLog <- SessionLogDao.update(q, Json.obj("indexed" -> false), multi = true)
      } yield {
        // mark all field to indexed = false
        val r = s"delete: ${deleteOk.body} <br/>createOk: ${createOk.body} <br/>mappingOk: ${mappingOk.body} <br/>updateLog: ${updateLog.stringify}"

        Ok(Html("done with result: <br/><br/><br/>" + r))
      }

    } else Future.successful(Ok("?"))
  }

  def pruneData(apiKey: String) = Authenticated(role = Some(UserProfile.ROLE_admin)).async { request =>

    if (apiKey == "Dkd9ea29ud") {
      for {
        result1 <- tryPerform(SessionHeaderDao.coll.drop)
        result2 <- tryPerform(SessionLogDao.coll.drop)
      } yield {
        Ok
      }
    } else Future.successful(Forbidden)
  }
}