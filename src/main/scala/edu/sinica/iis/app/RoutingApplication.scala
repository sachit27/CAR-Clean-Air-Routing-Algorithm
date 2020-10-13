package edu.sinica.iis.app

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, parameter, path, _}
import akka.pattern.ask
import akka.routing.BalancingPool
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.graphhopper.routing.util.EncodingManager
import com.graphhopper.util.Parameters
import com.graphhopper.{CARGraphHopper, GraphHopper}
import edu.sinica.iis.app.GoogleActor.GoogleQuery
import edu.sinica.iis.app.Router.{Flavor, RoutingRequest}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

/** The main program of this application.
  * Http request is processed(using akka http) and passed on the the Router actor.
  */
object RoutingApplication extends App {
  implicit val system = ActorSystem("car-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  val log = Logging(system.eventStream, RoutingApplication.getClass)

  val hopper: GraphHopper = new CARGraphHopper().forServer
  hopper.setDataReaderFile("map_cache/map.osm")
  // where to store graphhopper files?
  hopper.setGraphHopperLocation("graph_cache")
  hopper.setEncodingManager(new EncodingManager("car,foot,bike"))
  hopper.setCHEnabled(false)

  // now this can take minutes if it imports or a few seconds for loading
  // of course this is dependent on the area you import
  hopper.importOrLoad

  val routerManager = system.actorOf(RouterManager.props)
  val google = system.actorOf(BalancingPool(5).props(GoogleActor.props))

  val route =
    path("car") {
      get {
        parameter("latFrom".as[Double], "lonFrom".as[Double], "latTo".as[Double], "lonTo".as[Double], "mode".as[String]) {
          (latFrom, lonFrom, latTo, lonTo, mode) =>
            implicit val timeout: Timeout = 40.seconds
            val flavor = Flavor("cleanest", Parameters.Algorithms.ASTAR)
            val result: Future[String] =
              (routerManager ? RoutingRequest(latFrom, lonFrom, latTo, lonTo, mode, flavor, hopper)).mapTo[String]
            complete(result)
        }

      }
    } ~
      path("fastest") {
        get {
          parameter("latFrom".as[Double], "lonFrom".as[Double], "latTo".as[Double], "lonTo".as[Double], "mode".as[String]) {
            (latFrom, lonFrom, latTo, lonTo, mode) =>
              implicit val timeout: Timeout = 20.seconds
              val flavor = Flavor("fastest", Parameters.Algorithms.ASTAR_BI)
              val result: Future[String] =
                (routerManager ? RoutingRequest(latFrom, lonFrom, latTo, lonTo, mode, flavor, hopper)).mapTo[String]
              complete(result)
          }

        }
      } ~
      path("shortest") {
        get {
          parameter("latFrom".as[Double], "lonFrom".as[Double], "latTo".as[Double], "lonTo".as[Double], "mode".as[String]) {
            (latFrom, lonFrom, latTo, lonTo, mode) =>
              implicit val timeout: Timeout = 20.seconds
              val flavor = Flavor("shortest", Parameters.Algorithms.ASTAR_BI)
              val result: Future[String] =
                (routerManager ? RoutingRequest(latFrom, lonFrom, latTo, lonTo, mode, flavor, hopper)).mapTo[String]
              complete(result)
          }

        }
      } ~
      path("google") {
        get {
          parameter("from".as[String], "to".as[String], "mode".as[String]) {
            (from, to, mode) =>
              implicit val timeout: Timeout = 20.seconds

              val result: Future[String] =
                (google ? GoogleQuery(from, to, mode)).mapTo[String]
              complete(result)
          }

        }
      }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
  log.info(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
