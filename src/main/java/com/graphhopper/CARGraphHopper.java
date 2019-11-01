package com.graphhopper;

import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.CleanestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;

public class CARGraphHopper extends GraphHopperOSM {
    @Override
    public Weighting createWeighting(HintsMap hintsMap, FlagEncoder encoder, Graph graph) {
        String weightingStr = hintsMap.getWeighting().toLowerCase();
        if ("cleanest".equals(weightingStr)) {
            return new CleanestWeighting(encoder);
        } else {
            return super.createWeighting(hintsMap, encoder, graph);
        }
    }
}
