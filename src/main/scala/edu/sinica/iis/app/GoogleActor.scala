package edu.sinica.iis.app

import akka.actor.{Actor, ActorLogging, Props}
import com.graphhopper.GraphHopper
import edu.sinica.iis.app.GoogleActor.GoogleQuery
import edu.sinica.iis.util.Google.QueryResult
import edu.sinica.iis.util.ResponseInterface.{Direction, EvaluationResponse, EvaluationResponseString, OK, Point, Response, Route}
import edu.sinica.iis.util.{Google, Pm25Calculator, ResponseInterface}

object GoogleActor {
  def props: Props = Props[GoogleActor]
  case class GoogleQuery(from: String, to:String, mode: String, simpleResponse: Boolean = false)
}
class GoogleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case GoogleQuery(from, to, mode, simpleResponse) =>
      val resultSet = Google.query(from ,to, mode)
      val recreateRoute = Google.recreate(resultSet.route)
      val totalPM25 = Pm25Calculator.calculateGoogle(recreateRoute)

      if (simpleResponse) {
        log.info("Google finished.")
        val evalResponse = EvaluationResponse(OK, from + " " + to, resultSet.distance,
          resultSet.duration, totalPM25, 0, Point(0, 0), Point(0, 0), mode)
        sender() ! ResponseInterface.toJson(evalResponse).toString
      } else {
        val route = Route(List(Direction(0, 0, "", "", 0)),
          QueryResult(recreateRoute, resultSet.distance, resultSet.duration).toPointList,
          resultSet.distance, resultSet.duration, totalPM25)
        val response = Response(OK, "", Some(route))
        log.info("Google finished. " + response.toString)
        sender() ! ResponseInterface.toJson(response).toString
      }


  }
}
