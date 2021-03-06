package edu.sinica.iis.app

import akka.actor.ActorSystem
import akka.routing.BalancingPool
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.graphhopper.{CARGraphHopper, GraphHopper}
import com.graphhopper.routing.util.EncodingManager
import edu.sinica.iis.app.GoogleActor.GoogleQuery
import edu.sinica.iis.app.RecorderActor.{Protocol, ProtocolGoogle}
import edu.sinica.iis.app.Router.RoutingRequest
import edu.sinica.iis.app.RoutingApplication.{google, system}
import edu.sinica.iis.util.ResponseInterface.Point

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object EvaluationGoogle {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("car-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val recorder = system.actorOf(RecorderActor.props("eval_google_new_08.json"))
    val google = system.actorOf(BalancingPool(5).props(GoogleActor.props))


    val pointOfInterests = List(Point(25.042331, 121.614516),
      Point(25.055222,121.617254),
      Point(25.021209, 121.540208),
      Point(25.046730, 121.515516),
      Point(25.037632, 121.564452),
      Point(25.034139, 121.564461),
      Point(24.986799, 121.575685),
      Point(25.038065, 121.522528),
      Point(25.014596, 121.463296),
      Point(25.088914, 121.524424),
      Point(25.037357, 121.499911),
      Point(25.040296, 121.511998),
      Point(25.102598, 121.548428),
      Point(25.025633, 121.561650),
      Point(25.124200, 121.513589),
      Point(25.040834, 121.519001),
      Point(25.072626, 121.524907),
      Point(25.072626, 121.524907),
      Point(25.033422, 121.535200),
      Point(24.997285, 121.585183))

    val pointOfInterestsTest = List(Point(25.042331, 121.614516),
      Point(25.055222,121.617254),
      Point(25.021209, 121.540208))

    val stringfy = pointOfInterests.map(a => a.lat + "," + a.lon)

    val locationPairs = stringfy.combinations(2).toList

    val modeList = List("car", "foot", "bike")
    for (loc <- locationPairs; mode <- modeList) {
      recorder ! ProtocolGoogle(google, GoogleQuery(loc(0), loc(1), mode, true))
      recorder ! ProtocolGoogle(google, GoogleQuery(loc(1), loc(0), mode, true))
    }

  }
}
