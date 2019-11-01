import com.graphhopper.*;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JavaSample {
    public static void main(String[] args) {
        // create one GraphHopper instance
        GraphHopper hopper = new CARGraphHopper().forServer();
        hopper.setDataReaderFile("map_cache/data.osm");
        // where to store graphhopper files?
        hopper.setGraphHopperLocation("graph_cache");
        hopper.setEncodingManager(new EncodingManager("car"));
        hopper.setCHEnabled(false);

        // now this can take minutes if it imports or a few seconds for loading
        // of course this is dependent on the area you import
        hopper.importOrLoad();

        // simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
        GHRequest req = new GHRequest(25.048875, 121.514204
                , 25.055222,121.617254).
                setWeighting("cleanest").
                setVehicle("car").
                setLocale(Locale.US).
                setAlgorithm(Parameters.Algorithms.ASTAR);

        //System.out.println(req);
        final long startTime = System.currentTimeMillis();
        GHResponse rsp = hopper.route(req);
        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime)/1000 + " seconds." );

        // first check for errors
        if(rsp.hasErrors()) {
            // handle them!
            // rsp.getErrors()
            System.out.println(rsp.getErrors());
            return;
        }

        // use the best path, see the GHResponse class for more possibilities.
        PathWrapper path = rsp.getBest();

        System.out.println("HA HA" + path.getInstructions());

        // points, distance in meters and time in millis of the full path
        PointList pointList = path.getPoints();
        System.out.println(pointList);
        double distance = path.getDistance();
        System.out.println("Distance: " + distance);
        long timeInMs = path.getTime();
        System.out.println("Time: " + timeInMs);

        InstructionList il = path.getInstructions();
        // iterate over every turn instruction
        for(Instruction instruction : il) {
            instruction.getDistance();
        }

        // or get the json
        List<Map<String, Object>> iList = il.createJson();
        System.out.println(iList);

        // or get the result as gpx entries:
        List<GPXEntry> list = il.createGPXList();
    }
}
