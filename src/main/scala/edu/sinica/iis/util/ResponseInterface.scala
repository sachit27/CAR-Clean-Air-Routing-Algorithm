package edu.sinica.iis.util

import akka.io.Tcp.Message
import edu.sinica.iis.util.ResponseInterface.EvaluationResponse
import play.api.libs.json.{JsString, JsValue, Json, Writes}

object ResponseInterface {

  trait Status
  case object OK extends Status
  case object ZERO_RESULTS extends Status
  case object SERVER_ERROR extends Status
  case object OUT_OF_BOUND extends Status

  case class Point(lat: Double, lon: Double)
  case class Direction(time: Double, distance: Double, instruction: String, name: String, code: Int)

  /** The main returning data
    * @param direction
    * @param path
    * @param totalDistance distance in meter
    * @param totalTime duration in second
    * @param totalPM25 pm25 exposure in microgram / m squared
    */
  case class Route(direction: Seq[Direction], path: Seq[Point], totalDistance: Double,
                   totalTime: Double, totalPM25: Double)
  case class Response(status: Status, message: String, route: Option[Route])
  case class EvaluationResponse(status: Status, message: String, totalDistance: Double,
                                totalTime: Double, totalPM25: Double, executionTime: Double,
                                startingPoint: Point, destination: Point, mode: String)
  case class EvaluationResponseString(status: Status, message: String, totalDistance: Double,
                                totalTime: Double, totalPM25: Double, executionTime: Double,
                                startingPoint: String, destination: String, mode: String)

  def processInstruction(instruction: String, name: String) = {
    instruction.replace(name, "").capitalize
  }

  implicit val pointWrites = new Writes[Point] {
    override def writes(o: Point): JsValue = Json.arr(o.lat, o.lon)
  }

  implicit val statusWrites = new Writes[Status] {
    override def writes(s: Status): JsValue = JsString(s.toString)
  }

  implicit val directionWrites = new Writes[Direction] {
    def writes(direction: Direction) = Json.obj(
      "time" -> direction.time,
      "distance" -> direction.distance,
      "instruction" -> direction.instruction,
      "name" -> direction.name,
      "code" -> direction.code
    )
  }

  implicit val routeWrites = new Writes[Route] {
    def writes(route: Route) = Json.obj(
      "direction" -> route.direction,
      "path" -> route.path,
      "total_distance" -> route.totalDistance,
      "total_time" -> route.totalTime,
      "total_pm25" -> route.totalPM25
    )
  }

  implicit val responseWrites = new Writes[Response] {
    def writes(response: Response) = Json.obj(
      "status" -> response.status,
      "message" -> response.message,
      "route" -> response.route
    )
  }

  implicit val evalResponseWrites = new Writes[EvaluationResponse] {
    def writes(evalResponse: EvaluationResponse) = Json.obj(
      "status" -> evalResponse.status,
      "message" -> evalResponse.message,
      "totalDistance" -> evalResponse.totalDistance,
      "totalTime" -> evalResponse.totalTime,
      "totalPM25" -> evalResponse.totalPM25,
      "executionTime" -> evalResponse.executionTime,
      "startingPoint" -> evalResponse.startingPoint,
      "destination" -> evalResponse.destination,
      "mode" -> evalResponse.mode
    )
  }

  def toJson(response: Response) = Json.toJson(response)
  def toJson(evalResponse: EvaluationResponse) = Json.toJson(evalResponse)
}
