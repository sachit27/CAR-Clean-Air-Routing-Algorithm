package edu.sinica.iis.util

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, JsPath, Json, Reads}
import com.graphhopper.util.{DistanceCalc2D, DistanceCalcEarth}

import scala.io.Source


object Test {
  val data = parseForecastFile("forecast_cache/forecast_20180512_12.json")

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

  def interpolate(lat: Double, lon: Double, dataSize: Int) = {

    time {
      val inverse = (1 to dataSize).map(a => 1 / a * a + 0.1)
      inverse.sum
    }
    val inverse = (1 to dataSize).map(a => 1 / a * a + 0.1)

    //val element = data.sortBy(entry => 1 / distance(entry, lat, lon)).take(10)
    //val inverse = element.map(a => 1 / (distance(a, lat, lon) + 0.1))
/*
    val hour = Math.floor(seconds / 3600).toInt
    val secondsLeft = seconds % 3600

    val weighted = element.zip(inverse).map{a =>
      val lower = a._1.query(hour)
      val higher = a._1.query(hour + 1)
      ((secondsLeft / 3600) * higher + (1 - secondsLeft / 3600) * lower) * a._2
    }*/

    inverse.sum

  }

  def interpolateKriging(lat: Double, lon: Double, dataSize: Int) = {
    time {
      for (i <- 0 to dataSize / 2) {
        val element = (1 to dataSize/ 2).map(entry => 5 * (1 - Math.exp(-(12 / 7) * (12 / 7))))
      }
    }
    10.1

  }

  def interpolateSpline(lat: Double, lon: Double, seconds: Double) = {
    for (i <- 0 to data.size) {
      val element = data.map(entry => 5 * (1 - Math.exp(-(12/7) * (12/7))))
    }
    10.1

  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println((t1 - t0)/1000)
    result
  }

  def main(args: Array[String]): Unit = {
/*
      for (i <- 1000 to 100000 by 1000) {
        interpolate(25.161255, 121.419357, i)
      }*/

      for (i <- 1000 to 100000 by 1000) {
        interpolateKriging(25.161255, 121.419357, i)
      }

  }
}
