package edu.sinica.iis.app

import java.util.Locale

import akka.actor.{Actor, ActorLogging, Props}
import com.graphhopper.util.TranslationMap
import com.graphhopper.util.exceptions.{ConnectionNotFoundException, PointOutOfBoundsException}
import com.graphhopper.{GHRequest, GraphHopper}
import edu.sinica.iis.app.Router.RoutingRequest
import edu.sinica.iis.util.ResponseInterface.{Direction, EvaluationResponse, OK, OUT_OF_BOUND, Point, Response, Route, SERVER_ERROR, ZERO_RESULTS}
import edu.sinica.iis.util.{Pm25Calculator, ResponseInterface}

import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Router {
  def props: Props = Props[Router]

  /**
    *
    * @param weight The optimization goal of the algorithm.
    *               There are three possible weight: fastest, shortest, cleanest
    * @param algorithm The path finding algorithm to use
    *                  Most frequently use ones are: astar, astarbi, dijkstra, dijkstrabi
    */
  case class Flavor(weight: String, algorithm: String, simpleResponse: Boolean = false)
  case class RoutingRequest(latFrom: Double, lonFrom: Double, latTo: Double,
                            lonTo: Double, transport: String, option: Flavor, hopper: GraphHopper)
}

/** This actor handles the real routing job and sent back a json as a response */
class Router extends Actor with ActorLogging {
  override def receive: Receive = {
    case r@RoutingRequest(latFrom, lonFrom, latTo, lonTo, transport, option, hopper) =>

      val req = new GHRequest(latFrom, lonFrom,
        latTo, lonTo).setWeighting(option.weight).
        setVehicle(transport).
        setLocale(Locale.US).
        setAlgorithm(option.algorithm)

      val startTime = System.currentTimeMillis
      val possibleResponse = Try(hopper.route(req))
      val endTime = System.currentTimeMillis
      log.info("Total execution time: " + (endTime - startTime) / 1000 + " seconds.")

      possibleResponse match {
        case Success(rsp) => {
          // first check for errors
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
            sender() ! ResponseInterface.toJson(response).toString
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
              else Pm25Calculator.calculate(pointList.head, pointList.tail, transport)

            val route = Route(directions, pointList, distance, time, pm25)

            val response = Response(OK, "", Some(route))

            log.info("Finish request: " + r)
            if (option.simpleResponse) {
              val evalResponse = EvaluationResponse(OK, "", distance, time, pm25,
                (endTime - startTime) / 1000, Point(latFrom, lonFrom), Point(latTo, lonTo), transport)
              sender() ! ResponseInterface.toJson(evalResponse).toString
            } else {
              sender() ! ResponseInterface.toJson(response).toString
            }


          }
        }
        case Failure(throwable) => {
          val response = Response(SERVER_ERROR, throwable.getMessage, None)
          sender() ! ResponseInterface.toJson(response).toString
        }
      }







  }
}
