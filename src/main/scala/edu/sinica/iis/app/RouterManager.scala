package edu.sinica.iis.app

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.routing.BalancingPool
import com.graphhopper.{CARGraphHopper, GraphHopper}
import com.graphhopper.routing.util.EncodingManager
import edu.sinica.iis.app.Router.RoutingRequest

import scala.concurrent.duration._

object RouterManager {
  def props: Props = Props[RouterManager]
}

class RouterManager extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _ => Restart
    }

  val router = context.actorOf(BalancingPool(5).props(Router.props))

  override def receive: Receive = {
    case r@RoutingRequest(latFrom, lonFrom, latTo, lonTo, transport, option, hopper) =>
      log.info("Receive request")
      router forward r
  }
}
