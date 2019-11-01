package edu.sinica.iis.app

import java.io.{BufferedWriter, File, FileWriter}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import edu.sinica.iis.app.GoogleActor.GoogleQuery
import edu.sinica.iis.app.RecorderActor.{Protocol, ProtocolGoogle}
import edu.sinica.iis.app.Router.RoutingRequest


object RecorderActor {
  def props(fileName: String): Props = Props(classOf[RecorderActor], fileName)
  case class Protocol(actorRef: ActorRef, content: RoutingRequest)
  case class ProtocolGoogle(acterRef: ActorRef, content: GoogleQuery)
}

/** This actor records information returned by an actor */
class RecorderActor(fileName: String) extends Actor with ActorLogging {

  val buf = scala.collection.mutable.ListBuffer.empty[String]
  // FileWriter
  val file = new File(fileName)
  val bw = new BufferedWriter(new FileWriter(file))

  var numRequest = 0
  var numReturned = 0



  override def receive: Receive = {
    case p@Protocol(actorRef, RoutingRequest(latFrom, lonFrom, latTo, lonTo, transport, option, hopper)) =>
      numRequest += 1
      actorRef ! p.content
    case g@ProtocolGoogle(actorRef, GoogleQuery(start, dest, transport, simpleResponse)) =>
      numRequest += 1
      actorRef ! g.content
    case response: String =>
      log.info("Get response: " + response)
      bw.write(response + "\n")
      bw.flush()
      numReturned += 1
      log.info("Finished tasks: " + numReturned + "/" + numRequest)
      if (numReturned == numRequest) {
        bw.close()
        context.stop(self)
        context.system.terminate()
      }
  }
}
