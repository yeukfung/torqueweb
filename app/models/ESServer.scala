package models

import play.api.Play
import am.libs.es.ESClient

object ES {
  val esSvrHost = Play.current.configuration.getString("es.server") getOrElse ("http://localhost:9200")
  val esClient = new ESClient(esSvrHost)
}