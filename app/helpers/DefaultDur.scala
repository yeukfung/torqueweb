package helpers

import scala.concurrent.duration.Duration

trait DefaultDur {
  implicit val dur: Duration = Duration(5, "seconds")
}
