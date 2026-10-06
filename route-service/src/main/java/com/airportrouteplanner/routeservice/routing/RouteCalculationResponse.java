package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraph;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record RouteCalculationResponse(
        RouteType routeType,
        String originAirportCode,
        String destinationAirportCode,
        double totalDistanceKm,
        double totalFuelLitres,
        double fuelPerPassengerLitres,
        Double totalJourneyMinutes,
        Instant journeyStartUtc,
        Instant journeyArrivalUtc,
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
                null,
                null,
                result.edges().stream()
                        .map(edge -> RouteLegResponse.fromStatic(graph, edge, calculator))
                        .toList());
    }

    public static RouteCalculationResponse from(
            RouteGraph graph,
            FastestRouteResult result,
            StaticRouteCalculator calculator) {
        return new RouteCalculationResponse(
                RouteType.FASTEST,
                result.originAirportCode(),
                result.destinationAirportCode(),
                result.totalDistanceKm(),
                result.totalFuelLitres(),
                result.fuelPerPassengerLitres(),
                FlightDurationCalculator.toMinutes(Duration.between(
                        result.journeyStartUtc(), result.journeyArrivalUtc())),
                result.journeyStartUtc(),
                result.journeyArrivalUtc(),
                result.legs().stream()
                        .map(leg -> RouteLegResponse.fromFastest(graph, leg, calculator))
                        .toList());
    }
}
