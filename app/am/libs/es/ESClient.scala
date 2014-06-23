package am.libs.es

import scala.concurrent.Future
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.json.JsObject
import play.api.libs.json.Json

class ESClient(esURL: String) {

  def baseUrl(idx: String, t: String, action: String) = esURL + s"/$idx/$t/$action".replaceAll("///", "/").replaceAll("//", "/")

  def bulk(index: Option[String] = None, t: Option[String] = None, data: JsObject): Future[Response] = {
    val url = baseUrl(index.getOrElse(""), t.getOrElse(""), "_bulk")
    WS.url(url).post(data)
  }

  def count(indices: Seq[String], types: Seq[String], query: String): Future[Response] = {
    val url = baseUrl(indices.mkString(","), types.mkString(","), "_count")
    WS.url(url).get
  }

  def createIndex(name: String, settings: Option[JsObject] = None): Future[Response] = {
    val url = s"$esURL/$name"
    settings match {
      case Some(js) => WS.url(url).put(js)
      case None => WS.url(url).put(Json.obj())
    }
  }

  def deleteIndex(name: String): Future[Response] = {
    val url = s"$esURL/$name"
    WS.url(url).delete()
  }

  def get(index: String, `type`: String, id: String): Future[Response] = {
    val url = baseUrl(index, `type`, id)
    WS.url(url).get
  }

  def getMapping(indices: Seq[String], types: Seq[String]): Future[Response] = {
    val url = baseUrl(indices.mkString(","), "_mapping", types.mkString(","))
    WS.url(url).get
  }

  def index(index: String, `type`: String, id: String, data: JsObject, refresh: Boolean = false): Future[Response] = {
    val url = baseUrl(index, `type`, id)
    if(id == "_mapping") println(s"url = $url with data: $data")
    WS.url(url).put(data)
  }

  def refresh(index: String) = {
    val url = s"$esURL/$index"
    WS.url(url).post(Json.obj())
  }

  def search(index: String, query: JsObject): Future[Response] = {
    val url = s"$esURL/$index/_search"
    WS.url(url).post(query)
  }

  //  /**
  //   * Query ElasticSearch for it's health.
  //   *
  //   * @param indices Optional list of index names. Defaults to empty.
  //   * @param level Can be one of cluster, indices or shards. Controls the details level of the health information returned.
  //   * @param waitForStatus One of green, yellow or red. Will wait until the status of the cluster changes to the one provided, or until the timeout expires.
  //   * @param waitForRelocatingShards A number controlling to how many relocating shards to wait for.
  //   * @param waitForNodes The request waits until the specified number N of nodes is available. Is a string because >N and ge(N) type notations are allowed.
  //   * @param timeout A time based parameter controlling how long to wait if one of the waitForXXX are provided.
  //   */
  //  def health(
  //    indices: Seq[String] = Seq.empty[String], level: Option[String] = None,
  //    waitForStatus: Option[String] = None, waitForRelocatingShards: Option[String] = None,
  //    waitForNodes: Option[String] = None, timeout: Option[String] = None): Future[Response] = {
  //    val req = url(esURL) / "_cluster" / "health" / indices.mkString(",")
  //
  //    val paramNames = List("level", "wait_for_status", "wait_for_relocation_shards", "wait_for_nodes", "timeout")
  //    val params = List(level, waitForStatus, waitForRelocatingShards, waitForNodes, timeout)
  //
  //    // Fold in all the parameters that might've come in.
  //    val freq = paramNames.zip(params).foldLeft(req)(
  //      (r, nameAndParam) => {
  //        nameAndParam._2.map({ p =>
  //          r.addQueryParameter(nameAndParam._1, p)
  //        }).getOrElse(r)
  //      })
  //
  //    doRequest(freq.GET)
  //  }

  //  /**
  //   * Query ElasticSearch Stats. Parameters to enable non-default stats as desired.
  //   *
  //   * @param indices Optional list of index names. Defaults to empty.
  //   * @param clear Clears all the flags (first).
  //   * @param refresh refresh stats.
  //   * @param flush flush stats.
  //   * @param merge merge stats.
  //   * @param warmer Warmer statistics.
  //   */
  //  def stats(indices: Seq[String] = Seq(), clear: Boolean = false, refresh: Boolean = false, flush: Boolean = false, merge: Boolean = false, warmer: Boolean = false): Future[Response] = {
  //    val req = indices match {
  //      case Nil => url(esURL) / "_stats"
  //      case _ => url(esURL) / indices.mkString(",") / "_stats"
  //    }
  //
  //    val paramNames = List("clear", "refresh", "flush", "merge", "warmer")
  //    val params = List(clear, refresh, flush, merge, warmer)
  //    val reqWithParams = paramNames.zip(params).filter(_._2).foldLeft(req)((r, nameAndParam) => r.addQueryParameter(nameAndParam._1, nameAndParam._2.toString))
  //
  //    doRequest(reqWithParams.GET)
  //  }  
}