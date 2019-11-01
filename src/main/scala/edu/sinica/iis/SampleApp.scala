package edu.sinica.iis

import java.util.Locale

import com.graphhopper.routing.util.EncodingManager
import com.graphhopper.util.{InstructionList, Parameters, PointList, TranslationMap}
import com.graphhopper._
import edu.sinica.iis.app.Router.Flavor
import edu.sinica.iis.util.{Pm25Calculator, ResponseInterface}
import edu.sinica.iis.util.ResponseInterface.{Direction, OK, Point, Response, Route, SERVER_ERROR}

import scala.collection.JavaConverters._


object SampleApp extends App {
  // create one GraphHopper instance

  val hopper = new CARGraphHopper().forServer
  hopper.setDataReaderFile("map_cache/map.osm")
  // where to store graphhopper files?
  hopper.setGraphHopperLocation("graph_cache")
  hopper.setEncodingManager(new EncodingManager("car,foot,bike"))
  hopper.setCHEnabled(false)

  // now this can take minutes if it imports or a few seconds for loading
  // of course this is dependent on the area you import
  hopper.importOrLoad

  val option = Flavor("cleanest", Parameters.Algorithms.ASTAR)

  // simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
  val req = new GHRequest(25.042331, 121.614516,
    25.046730, 121.515516).setWeighting(option.weight).
    setVehicle("bike").
    setLocale(Locale.US).
    setAlgorithm(option.algorithm)

  val startTime = System.currentTimeMillis
  val rsp = hopper.route(req)
  val endTime = System.currentTimeMillis
  println("Total execution time: " + (endTime - startTime) / 1000 + " seconds.")

  // first check for errors
  if (rsp.hasErrors) { // handle them!
    // rsp.getErrors()

    val response = Response(SERVER_ERROR, rsp.getErrors.toString, None)
    println(response)
    System.exit(1)
  }

  val path = rsp.getBest
  //val qq = for (instruction <- path.getInstructions) println(instruction)

  val translation = new TranslationMap().doImport.get("en")
  val il = path.getInstructions.iterator().asScala.toList
  val directions = for (instruction <- il)
    yield Direction(instruction.getTime, instruction.getDistance,
      ResponseInterface.processInstruction(instruction.getTurnDescription(translation), instruction.getName),
      instruction.getName, instruction.getSign)


  // points, distance in meters and time in millis of the full path
  val pointList = path.getPoints.iterator().asScala.map(p => Point(p.lat, p.lon)).toList

  val distance = path.getDistance

  val time = path.getTime

  val pm25 =
    if (option.weight == "cleanest") path.getRouteWeight
    else Pm25Calculator.calculate(pointList.head, pointList.tail)

  val route = Route(directions, pointList, distance, time, pm25)
  val response = Response(OK, "", Some(route))
  println(ResponseInterface.toJson(response))
}
