package com.airportrouteplanner.routeservice.routing;

import java.time.Instant;
import java.util.List;

public record EarliestArrivalPath(
        Instant journeyStartUtc,
        Instant journeyArrivalUtc,
        List<TimedRouteLeg> legs) {

    public EarliestArrivalPath {
        legs = List.copyOf(legs);
    }
}
