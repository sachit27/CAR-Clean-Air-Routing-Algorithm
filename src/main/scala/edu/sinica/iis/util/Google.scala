package edu.sinica.iis.util

import java.net.{URI, URLEncoder}
import java.util.Locale

import com.google.maps.model.EncodedPolyline
import com.graphhopper.util.exceptions.{ConnectionNotFoundException, PointOutOfBoundsException}
import com.graphhopper.{GHRequest, GraphHopper}
import edu.sinica.iis.util.ResponseInterface.{OUT_OF_BOUND, Point, Response, SERVER_ERROR, ZERO_RESULTS}
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Success, Try}
import collection.JavaConverters._

object Google {
  case class QueryResult(route: List[Step], distance: Double, duration: Double) {
    def toPointList = {
      val firstPoint = route.head.startPoint
      val tail = route.map(_.endPoint)
      firstPoint +: tail
    }
  }
  case class Step(startPoint: Point, endPoint: Point, distance: Double, duration: Double, intermidiate: String="") {

    /** DEPRECATED: Insert intermediate points between startPoint and endPoint mainly for
      * calculating more accurate PM2.5 along the path
      * @param n number of points to insert
      * @return intermediate points include endPoint
      */
    def insert(n: Int) = {
      val deltaLat = endPoint.lat - startPoint.lat
      val deltaLon = endPoint.lon - startPoint.lon

      val stepLat = deltaLat / (n + 1)
      val stepLon = deltaLon / (n + 1)

      val nodeList = for (i <- 1 to n + 1) yield {
        Point(startPoint.lat + i * stepLat, startPoint.lon + i * stepLon)
      }
      nodeList
    }

  }

  /**
    *
    * @param start the starting location. Can be coordinates like 24.156944,120.665573 or
    *              name like Academia Sinica
    * @param destination the starting location. Can be coordinates like 24.156944,120.665573 or
    *              name like Academia Sinica
    * @param transport the mode of transportation. Three modes possible: car, foot, bike
    * @return a QueryResult object
    */
  def query(start:String, destination: String, transport: String) = {

    // Four modes possible for google api: driving, walking, bicycling, transit
    val transportMapper = Map("car" -> "driving", "foot" -> "walking").withDefaultValue("driving")

    val urlSafeFrom = URLEncoder.encode(start, "UTF-8")
    val urlSafeTo = URLEncoder.encode(destination, "UTF-8")
    val url = s"https://maps.googleapis.com/maps/api/directions/json?" +
      s"origin=${urlSafeFrom}&destination=${urlSafeTo}&mode=${transportMapper(transport)}&key=AIzaSyC9L5sFNkXtsJ8IWGfwgbT-E6_T6jwf3iY"
    val data = scala.io.Source.fromURL(url).mkString
    val json = Json.parse(data)
    val totalDistance = (((json \ "routes")(0) \ "legs")(0) \ "distance" \ "value").as[Int] // in meters
    val totalDuration = (((json \ "routes")(0) \ "legs")(0) \ "duration" \ "value").as[Int] // in seconds


    val stepsJS = (((json \ "routes")(0) \ "legs")(0) \ "steps").as[List[JsObject]]
    val stepsScala = stepsJS.map {
      step =>
        val pointLats = (step \ "start_location" \ "lat").as[Double]
        val pointLons = (step \ "start_location" \ "lng").as[Double]
        val pointLat = (step \ "end_location" \ "lat").as[Double]
        val pointLon = (step \ "end_location" \ "lng").as[Double]
        val distance = (step \ "distance" \ "value").as[Double]
        val duration = (step \ "duration" \ "value").as[Double]
        val polyline = (step \ "polyline" \ "points").as[String]
        Step(Point(pointLats, pointLons), Point(pointLat, pointLon), distance, duration, polyline)
    }

    QueryResult(stepsScala, totalDistance, totalDuration)
  }

  /** Recreate route returned by Google api with polyline.
    * This is necessary because we need detailed paths to accurately
    * estimate the PM2.5 exposure.
    * @param steps
    * @return recreated list of steps
    */
  def recreate(steps: List[Step]): List[Step] = {
    steps.flatMap { step =>
      val polyline = parsePolyline(step.intermidiate)
      val stepStart = polyline.dropRight(1)
      val stepEnd = polyline.tail

      val miniSteps = stepStart.zip(stepEnd)
      val numSteps = miniSteps.size
      val averageDuration = step.duration / numSteps
      miniSteps.map { ministep =>
        val miniDistance = Pm25Calculator.distance(ministep._1.lat, ministep._1.lon, ministep._2.lat, ministep._2.lon)
        Step(ministep._1, ministep._2, miniDistance, averageDuration)
      }
    }
  }


  /** DEPRECATED: Recreate route returned by Google api with Graphhopper fastest routing.
    * This is necessary because we need detailed paths to accurately
    * estimate the PM2.5 exposure.
    * @param steps
    * @return recreated list of steps
    */
  def recreate(steps: List[Step], mode: String, hopper: GraphHopper) = {
    steps.flatMap { step =>
      val req = new GHRequest(step.startPoint.lat, step.startPoint.lon,
        step.endPoint.lat, step.endPoint.lon).setWeighting("shortest").
        setVehicle(mode).
        setLocale(Locale.US).
        setAlgorithm("astarbi")
      val possibleResponse = Try(hopper.route(req))
      possibleResponse match {
        case Success(rsp) => {
          if (rsp.hasErrors) { // handle them!
            val error = rsp.getErrors().get(0)
            val response = error match {
              case pobe: PointOutOfBoundsException =>
                Response(OUT_OF_BOUND, pobe.getMessage + ". Please input points within Taiwan.", None)
              case cnfe: ConnectionNotFoundException =>
                Response(ZERO_RESULTS, cnfe.getMessage + ". Please try different destinations or starting locations.", None)
              case _ =>
                Response(SERVER_ERROR, error.getMessage, None)
            }
            List(step)
          } else {
            val path = rsp.getBest
            val pointList = path.getPoints.iterator().asScala.map(p => Point(p.lat, p.lon)).toList
            val pointStart = pointList.dropRight(1)
            val pointEnd = pointList.tail

            val miniSteps = pointStart.zip(pointEnd)
            val numSteps = miniSteps.size
            val averageDuration = step.duration / numSteps

            miniSteps.map { ministep =>
              val miniDistance = Pm25Calculator.distance(ministep._1.lat, ministep._1.lon, ministep._2.lat, ministep._2.lon)
              Step(ministep._1, ministep._2, miniDistance, averageDuration)
            }
          }
        }
        case Failure(throwable) => {
          List(step)
        }
      }
    }
  }

  /** Parse polyline string from Google Direction API
    *
    * @param polyString
    * @return list of Point
    */
  def parsePolyline(polyString: String) = {
    val polyline = new EncodedPolyline(polyString)
    polyline.decodePath().asScala.toList.map(googlePoint => Point(googlePoint.lat, googlePoint.lng))
  }
}
