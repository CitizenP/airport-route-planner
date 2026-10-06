package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.Duration;
import java.time.Instant;

public record TimedRouteLeg(
        RouteGraphEdge edge,
        Instant scheduledDepartureInstant,
        Instant adjustedDepartureInstant,
        Instant arrivalInstant,
        Duration waitingDuration,
        Duration flightDuration) {
}
