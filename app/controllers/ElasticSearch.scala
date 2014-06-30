package controllers

import play.api.mvc.Controller
import models.ES
import play.api.libs.ws.WS
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits._

object ElasticSearch extends Controller with MySecured {

  def esRedirect(urlpath: String) = Authenticated.async { implicit request =>

    val url = ES.esSvrHost + urlpath;
    println(url);
    val resp = request.method.toLowerCase() match {
      case "post" => WS.url(url).post(request.body.asJson.getOrElse(Json.obj()))
      case "put" => WS.url(url).put(request.body.asJson.getOrElse(Json.obj()))
      case "delete" => WS.url(url).delete()
      case _ => WS.url(url).get
    }

    resp.map(r => Ok(r.json))
  }

}