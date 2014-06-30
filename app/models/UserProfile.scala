package models

import play.api.libs.json.Json
import play.api.libs.Crypto
import daos.UserProfileDao
import scala.concurrent.Future
import play.api.cache.Cache

object UserProfile {

  implicit val fmt = Json.format[UserProfile]
  val dao = UserProfileDao

  /** supportive function **/
  def createUserWithPass(eml: String, pass: String, name: String, profileType: String = "normal") = {
    val passWithHash = Crypto.sign(pass)
    UserProfile(eml, passWithHash, name, profileType)
  }

  def getProfileByEmail(eml: String): Future[Option[UserProfile]] = {
    val q = Json.obj("eml" -> eml)
    dao.findFirstT(q)
  }

}

case class UserProfile(
  eml: String,
  passwordHash: String,
  name: String,
  profileType: String = "normal", // normal, admin, race
  id: Option[String] = None)


  