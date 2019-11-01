package edu.sinica.iis

import com.graphhopper.routing.util.{EncodingManager, FootFlagEncoder}
import com.graphhopper.storage.GraphBuilder

object Cute {
  def main(args: Array[String]): Unit = {
    //val encoder = new FootFlagEncoder()
    val em = new EncodingManager("foot")
    val gb = new GraphBuilder(em).setLocation("graph_cache").setStore(true)

    import com.graphhopper.storage.GraphStorage
    val graph = gb.load()
    //import com.graphhopper.storage.RAMDirectory
    import com.graphhopper.storage.index.LocationIndex
    //import com.graphhopper.storage.index.LocationIndexTree
    //val index = new LocationIndexTree(graph.getBaseGraph, new RAMDirectory("graphhopper_folder", true))
  }

}
