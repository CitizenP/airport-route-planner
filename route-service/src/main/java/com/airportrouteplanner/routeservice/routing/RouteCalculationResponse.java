package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraph;
import java.util.List;

public record RouteCalculationResponse(
        RouteType routeType,
        String originAirportCode,
        String destinationAirportCode,
        double totalDistanceKm,
        double totalFuelLitres,
        double fuelPerPassengerLitres,
        Long totalJourneyMinutes,
        List<RouteLegResponse> legs) {

    public RouteCalculationResponse {
        legs = List.copyOf(legs);
    }

    public static RouteCalculationResponse from(
            RouteGraph graph,
            StaticRouteResult result,
            StaticRouteCalculator calculator) {
        return new RouteCalculationResponse(
                result.routeType(),
                result.originAirportCode(),
                result.destinationAirportCode(),
                result.totalDistanceKm(),
                result.totalFuelLitres(),
                result.fuelPerPassengerLitres(),
                null,
                result.edges().stream()
                        .map(edge -> RouteLegResponse.from(graph, edge, calculator))
                        .toList());
    }
}
