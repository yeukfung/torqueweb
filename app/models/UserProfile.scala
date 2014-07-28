package models

import play.api.libs.json.Json
import play.api.libs.Crypto
import daos.UserProfileDao
import scala.concurrent.Future
import play.api.cache.Cache
import scala.concurrent.ExecutionContext.Implicits._

object UserProfile {

  implicit val fmt = Json.format[UserProfile]
  val dao = UserProfileDao

  /** supportive function **/
  def createUserWithPass(eml: String, pass: String, name: String, role: String = ROLE_normal) = {
    val passWithHash = Crypto.sign(pass)
    UserProfile(eml, passWithHash, name, role)
  }

  def getProfileByEmail(eml: String): Future[Option[UserProfile]] = {
    val q = Json.obj("eml" -> eml)
    dao.findFirstT(q)
  }

  def getProfileByDeviceId(encodedDeviceId: String): Future[Option[UserProfile]] = {
    val q = Json.obj("deviceIdEncoded" -> encodedDeviceId)
    dao.findFirstT(q)
  }

  def getByLoginData(eml: String, signedPass: String): Future[Option[UserProfile]] = {
    val q = Json.obj("eml" -> eml, "passwordHash" -> signedPass)
    dao.findFirstT(q)
  }

  val ROLE_normal = "normal"
  val ROLE_race = "race"
  val ROLE_admin = "admin"
}

case class UserProfile(
  eml: String,
  passwordHash: String,
  name: String,
  role: String = UserProfile.ROLE_normal, // normal, admin, race
  deviceId: Option[String] = None,
  deviceIdEncoded: Option[String] = None,
  disabled: Option[Boolean] = Some(false),
  id: Option[String] = None)


  