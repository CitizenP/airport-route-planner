package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.util.Comparator;

final class RouteEdgeOrdering {

    static final Comparator<RouteGraphEdge> DETERMINISTIC = Comparator
            .comparing(RouteGraphEdge::destinationAirportCode)
            .thenComparing(RouteGraphEdge::companyCode)
            .thenComparingInt(RouteGraphEdge::routeNumber)
            .thenComparingInt(edge -> directionOrder(edge.direction()))
            .thenComparingLong(RouteGraphEdge::companyRouteId);

    private RouteEdgeOrdering() {
    }

    static int comparePaths(java.util.List<RouteGraphEdge> left, java.util.List<RouteGraphEdge> right) {
        int commonSize = Math.min(left.size(), right.size());
        for (int index = 0; index < commonSize; index++) {
            int comparison = DETERMINISTIC.compare(left.get(index), right.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static int directionOrder(FlightDirection direction) {
        return switch (direction) {
            case OUTBOUND -> 0;
            case RETURN -> 1;
        };
    }
}
