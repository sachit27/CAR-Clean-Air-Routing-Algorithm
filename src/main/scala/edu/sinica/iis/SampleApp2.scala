package edu.sinica.iis

import java.util.Locale

import com.graphhopper.routing.util.EncodingManager
import com.graphhopper.util.{InstructionList, Parameters, PointList, TranslationMap}
import com.graphhopper._
import com.graphhopper.util.exceptions.{ConnectionNotFoundException, PointOutOfBoundsException}
import edu.sinica.iis.app.Router.Flavor
import edu.sinica.iis.util.{Pm25Calculator, ResponseInterface}
import edu.sinica.iis.util.ResponseInterface.{Direction, OK, OUT_OF_BOUND, Point, Response, Route, SERVER_ERROR, ZERO_RESULTS}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


object SampleApp2 extends App {
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

  val option = Flavor("fastest", Parameters.Algorithms.ASTAR_BI)

  // simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
  val req = new GHRequest(24.35540,121.61785,
  24.74218,121.61454).setWeighting(option.weight).
    setVehicle("car").
    setLocale(Locale.US).
    setAlgorithm(option.algorithm)

  val startTime = System.currentTimeMillis
  val possibleResponse = Try(hopper.route(req))
  val endTime = System.currentTimeMillis
  println("Total execution time: " + (endTime - startTime) / 1000 + " seconds.")

  possibleResponse match {
    case Success(rsp) => {
      // first check for errors
      if (rsp.hasErrors) { // handle them!
        val error = rsp.getErrors().get(0)
        println(error.getClass)
        val response = error match {
          case pobe: PointOutOfBoundsException =>
            Response(OUT_OF_BOUND, pobe.getMessage + ". Please input points within Taiwan.", None)
          case cnfe: ConnectionNotFoundException =>
            Response(ZERO_RESULTS, cnfe.getMessage + ". Please try different destinations or starting locations.", None)
          case _ =>
            Response(SERVER_ERROR, error.getMessage, None)
        }
        println(response)
      } else {

        val path = rsp.getBest

        val translation = new TranslationMap().doImport.get("en")
        val il = path.getInstructions.iterator().asScala.toList
        val directions = for (instruction <- il)
          yield Direction(instruction.getTime, instruction.getDistance,
            ResponseInterface.processInstruction(instruction.getTurnDescription(translation), instruction.getName),
            instruction.getName, instruction.getSign)



        // points, distance in meters and time in millis of the full path
        val pointList = path.getPoints.iterator().asScala.map(p => Point(p.lat, p.lon)).toList

        val distance = path.getDistance

        val time = path.getTime / 1000


        val pm25 =
          if (option.weight == "cleanest") path.getRouteWeight
          else Pm25Calculator.calculate(pointList.head, pointList.tail)

        val route = Route(directions, pointList, distance, time, pm25)

        val response = Response(OK, "", Some(route))

        println("Finish request: ")
        println(ResponseInterface.toJson(response).toString)
      }
    }
    case Failure(throwable) => {
      val response = Response(SERVER_ERROR, throwable.toString, None)
      println(response)
    }
  }

}

