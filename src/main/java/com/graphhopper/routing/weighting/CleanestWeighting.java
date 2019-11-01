package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;
import edu.sinica.iis.util.Interpolation;

public class CleanestWeighting extends AbstractWeighting {
    protected final static double SPEED_CONV = 3.6;

    public CleanestWeighting(FlagEncoder flagEncoder) {
        super(flagEncoder);
    }

    @Override
    public double getMinWeight(double currDistToGoal) {
        return currDistToGoal;
    }

    public double getMinWeightAir(double currDistToGoal, double latFrom, double lonFrom, double time) {
        double airQuality = Interpolation.interpolate(latFrom, lonFrom, time);
        return airQuality * currDistToGoal;
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        double speed = reverse ? flagEncoder.getReverseSpeed(edgeState.getFlags()) : flagEncoder.getSpeed(edgeState.getFlags());
        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edgeState.getDistance() / speed * SPEED_CONV;

        return time;
    }

    public double calcWeightAir(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId, double timeSoFar, NodeAccess nodeAccess) {
        int fromNode = edgeState.getBaseNode();
        double fromLat = nodeAccess.getLatitude(fromNode);
        double fromLon = nodeAccess.getLongitude(fromNode);
        double airQuality = Interpolation.interpolate(fromLat, fromLon, timeSoFar);

        return airQuality * edgeState.getDistance();
    }

    @Override
    public String getName() {
        return "cleanest";
    }

    @Override
    public String toString() {
        return "cleanest";
    }
}
