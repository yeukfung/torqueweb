package helpers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.json.JsArray
import play.api.libs.json.Format
import play.api.libs.json.Json

object MyHelper {
  

  
  def mapToQueryString(m: Map[String, Any]) = m.map(item => s"${item._1}=${item._2}&").foldLeft("")((item, acc) => acc + item)
  
  def tryPerform(f: () => Future[Boolean]): Future[Boolean] = {
    try {
      f().fallbackTo(Future(false))
    } catch {
      case _: Throwable => Future(false)
    }
  }

}