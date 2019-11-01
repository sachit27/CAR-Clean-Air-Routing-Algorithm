package edu.sinica.iis.util

import com.graphhopper.util.DistancePlaneProjection
import edu.sinica.iis.util.Google.Step
import edu.sinica.iis.util.Pm25Calculator.distance
import edu.sinica.iis.util.ResponseInterface.Point

object Pm25Calculator {
  case class Accumulator(lastPoint: Point, durationAcc: Double, pm25Acc: Double)
  case class AccumulatorGoogle(durationAcc: Double, pm25Acc: Double)

  /** Calculate the real earth distance given coordinates
    *
    * @param fromLat latitude of the first point, e.g. 25.042574
    * @param fromLon longtitude of the first point, e.g. 121.614649
    * @param toLat latitude of the second point, e.g. 25.055222
    * @param toLon latitude of the second point, e.g. 121.617254
    * @return earth distance in meters
    */
  def distance(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double) = {
    val EAR_DIST = new DistancePlaneProjection
    EAR_DIST.calcDist(fromLat, fromLon, toLat, toLon)
  }

  /** Calculate the total pm2.5 exposure given a path (list of points)
    *
    * @param pathHead the starting point of the path
    * @param path the remaining points after the first point
    * @return PM 2.5 exposure in the unit of m * microgram / cubic meter
    */

  def calculate(pathHead: Point, path: List[Point], mode: String = "car") = {
    val aggregation = path.foldLeft(Accumulator(pathHead, 0, 0)) {
      (l, r) =>
        // FIX ME: Use more accurate speed
        val speed =
          if (mode == "car") 15 // Estimate for car speed km/h
          else if (mode == "foot") 3
          else if (mode == "bike") 8
          else 10

        val dist = distance(r.lat, r.lon, l.lastPoint.lat, l.lastPoint.lon)

        val numInsert = Math.floor(dist * 0.01).toInt // 0.01 is an empirical result for reasonable node per meter

        val deltaLat = r.lat - l.lastPoint.lat
        val deltaLon = r.lon - l.lastPoint.lon

        val stepLat = deltaLat / (numInsert + 1)
        val stepLon = deltaLon / (numInsert + 1)

        val nodeList = for (i <- 1 to numInsert + 1) yield {
          Point(l.lastPoint.lat + i * stepLat, l.lastPoint.lon + i * stepLon)
        }

        val dura = dist / speed * 3.6

        val pm25Value = Interpolation.interpolate(l.lastPoint.lat, l.lastPoint.lon, l.durationAcc + dura)
        val pm25Exposure = pm25Value * dist

        Accumulator(r, l.durationAcc + dura, l.pm25Acc + pm25Exposure)
    }
    aggregation.pm25Acc
  }

  /** Calculate total PM 2.5 exposure based on the path.
    * @param route list of steps returned by Google Direction API
    * @return PM 2.5 exposure in the unit of m * microgram / cubic meter
    */
  def calculateGoogle(route: List[Step]) = {
    val aggregation = route.foldRight(AccumulatorGoogle(0, 0)) {
      (l, r) =>
        val numInsert = Math.floor(l.distance * 0.01).toInt // 0.01 is an empirical result for reasonable node per meter
        val nodeList = l.insert(numInsert)
        val durationFraction = l.duration / (numInsert + 1)

        val pm25List = nodeList.map(node => {
          Interpolation.interpolate(node.lat, node.lon, r.durationAcc + durationFraction)
        })
        //val pm25Value = Interpolation.interpolate(l.point.lat, l.point.lon, r.durationAcc + l.duration)

        val distanceFration = l.distance / (numInsert + 1)


        val pm25Exposure = pm25List.map(s => s * distanceFration).sum

        //Accumulator(Point(l.point.lat, l.point.lon), r.durationAcc + l.duration, r.pm25Acc + pm25Exposure)
        AccumulatorGoogle(r.durationAcc + l.duration, r.pm25Acc + pm25Exposure)
    }
    aggregation.pm25Acc
  }
}
