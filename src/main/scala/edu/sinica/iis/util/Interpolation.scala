package edu.sinica.iis.util

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, JsPath, Json, Reads}
import com.graphhopper.util.{DistanceCalc2D, DistanceCalcEarth}

import scala.io.Source


object Interpolation {
  val data = parseForecastFile("forecast_cache/forecast_20180508_12.json")

  case class Entry(devicdId: String, nowP0: Double, nowP1: Double,
                   nowP2: Double, nowP3: Double, nowP4: Double,
                   nowP5: Double, lat: Double, lon: Double) {

    /** Query with index. Make sure the value is nonnegative.
      * Otherwise, it will lead to some unexpected behaviors by the routing algorithm.
      *
      * @param index the index of the entry
      * @return PM2.5 value of a certain hour (microgram per cubic meter)
      */
    def query(index: Int) = index match {
      case 0 => Math.max(nowP0, 0.0)
      case 1 => Math.max(nowP1, 0.0)
      case 2 => Math.max(nowP2, 0.0)
      case 3 => Math.max(nowP3, 0.0)
      case 4 => Math.max(nowP4, 0.0)
      case 5 => Math.max(nowP5, 0.0)
      case _ => 0.0
    }
  }

  def parseForecastFile(filename: String) = {
    val fileContents = Source.fromFile(filename).getLines.mkString
    val json = Json.parse(fileContents)

    implicit val locationReads: Reads[Entry] = (
      (JsPath \ "device_id").read[String] and
        (JsPath \ "now").read[Double] and
        (JsPath \ "now+1h").read[Double] and
        (JsPath \ "now+2h").read[Double] and
        (JsPath \ "now+3h").read[Double] and
        (JsPath \ "now+4h").read[Double] and
        (JsPath \ "now+5h").read[Double] and
        (JsPath \ "LATITUDE").read[Double] and
        (JsPath \ "LONGITUDE").read[Double]
      )(Entry.apply _)

    val data = json.as[List[JsObject]]
      .map(jobj => jobj.validate[Entry])
      .filter(result => result.isSuccess)
      .map(result => result.getOrElse(Entry("DEFAULT", 0, 0, 0, 0, 0, 0, 0, 0)))

    data
  }

  def distance(entry: Entry, lat: Double, lon: Double) = {
    val DIST_2D = new DistanceCalc2D
    DIST_2D.calcDist(entry.lat, entry.lon, lat, lon)
  }

  def interpolate(lat: Double, lon: Double, seconds: Double) = {

    val element = data.minBy(entry => distance(entry, lat, lon))

    val hour = Math.floor(seconds / 3600).toInt
    val secondsLeft = seconds % 3600

    val lower = element.query(hour)
    val higher = element.query(hour + 1)

    (secondsLeft / 3600) * higher + (1 - secondsLeft / 3600) * lower
  }
}
