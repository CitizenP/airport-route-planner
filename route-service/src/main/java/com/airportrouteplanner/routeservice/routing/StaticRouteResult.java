package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.util.List;

public record StaticRouteResult(
        RouteType routeType,
        String originAirportCode,
        String destinationAirportCode,
        List<RouteGraphEdge> edges,
        double optimizedCost,
        double totalDistanceKm,
        double totalFuelLitres,
        double fuelPerPassengerLitres) {

    public StaticRouteResult {
        edges = List.copyOf(edges);
    }
}
