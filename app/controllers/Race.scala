package controllers

import play.api.mvc.Controller
import models.UserProfile
import scala.concurrent.Future
import scala.util.Random
import play.api.libs.json.Json
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.ActorRef
import helpers.GenSessionLogActor
import akka.actor.Props

object Race extends Controller with MySecured {

  def index = Authenticated(role = Some(UserProfile.ROLE_race)).async { implicit request =>
    Future.successful(Ok(views.html.race.index()))
  }

  def settings = Authenticated(role = Some(UserProfile.ROLE_race)).async { implicit request =>
    Future.successful(Ok(views.html.race.settings()))
  }

  // generator for demo purpose, only admin can start/stop the generation
  val testActor = Akka.system.actorOf(Props[GenSessionLogActor])
  
  def startGen(raceId: String) = Authenticated(role = Some(UserProfile.ROLE_admin)).async {
	testActor ! ("start", raceId)
    Future.successful(Ok("started"))
  }

  def stopGen(raceId: String) = Authenticated(role = Some(UserProfile.ROLE_admin)).async {
	testActor ! ("stop", raceId)
	Future.successful(Ok("stopped"))
  }
}

