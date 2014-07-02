package helpers

import play.api.Play
import play.Configuration

trait Cfg {
  def getString(s:String, defaultVal:String = "") = Play.current.configuration.getString(s) getOrElse defaultVal
}