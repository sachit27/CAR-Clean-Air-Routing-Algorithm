package edu.sinica.iis.util

import edu.sinica.iis.util.Google.{QueryResult, Step}
import edu.sinica.iis.util.ResponseInterface.Point
import org.scalatest.FlatSpec

class Pm25CalculatorSpec extends FlatSpec {
  "The distance function" should "calculate the earth distance (in meters)" in {
    val distance = Pm25Calculator.distance(25.042574, 121.614649, 25.055222, 121.617254)
    assert(Math.round(distance) == 1431)
  }

  "The calculate function" should "calculate the total pm2.5 exposure" in {
    val value =
      Pm25Calculator.calculate(Point(25.042574, 121.614649), List(Point(25.055222, 121.617254)))
    assert(Math.round(value) == 9584)
  }

  "The calculateGoogle function" should "calculate the total pm2.5 exposure" in {
    val resultSet = QueryResult(List(
      Step(Point(25.0431688,121.6144288),Point(25.0437198,121.6161922),189.0,68.0),
      Step(Point(25.0437198,121.6161922),Point(25.0453559,121.6178803),263.0,83.0),
      Step(Point(25.0453559,121.6178803),Point(25.0504637,121.6195314),599.0,100.0),
      Step(Point(25.0504637,121.6195314),Point(25.0548839,121.6216944),574.0,83.0),
      Step(Point(25.0548839,121.6216944),Point(25.0551162,121.6172357),463.0,99.0)),
      2088.0,433.0)

    val totalPM25 = Pm25Calculator.calculateGoogle(resultSet.route)
  }
}
