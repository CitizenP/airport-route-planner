package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.Instant;
import java.time.LocalTime;

public record RouteLegResponse(
        Long companyRouteId,
        int routeNumber,
        FlightDirection direction,
        String companyCode,
        Long aircraftTypeId,
        String originAirportCode,
        String destinationAirportCode,
        double distanceKm,
        double fuelLitres,
        double fuelPerPassengerLitres,
        LocalTime scheduledDepartureUtc,
        LocalTime adjustedDepartureUtc,
        int adjustedDepartureDayOffset,
        long delayMinutes,
        Instant scheduledDepartureInstant,
        Instant adjustedDepartureInstant,
        Instant arrivalInstant,
        Double waitingMinutes,
        Double flightDurationMinutes) {

    public static RouteLegResponse fromStatic(
            RouteGraph graph,
            RouteGraphEdge edge,
            StaticRouteCalculator calculator) {
        return new RouteLegResponse(
                edge.companyRouteId(),
                edge.routeNumber(),
                edge.direction(),
                edge.companyCode(),
                edge.aircraftTypeId(),
                edge.originAirportCode(),
                edge.destinationAirportCode(),
                edge.distanceKm(),
                calculator.fuelLitres(graph, edge),
                calculator.fuelPerPassengerLitres(graph, edge),
                edge.scheduledDepartureUtc(),
                edge.adjustedDepartureUtc(),
                edge.adjustedDepartureDayOffset(),
                edge.delayMinutes(),
                null,
                null,
                null,
                null,
                null);
    }

    public static RouteLegResponse fromFastest(
            RouteGraph graph,
            TimedRouteLeg leg,
            StaticRouteCalculator calculator) {
        RouteGraphEdge edge = leg.edge();
        return new RouteLegResponse(
                edge.companyRouteId(),
                edge.routeNumber(),
                edge.direction(),
                edge.companyCode(),
                edge.aircraftTypeId(),
                edge.originAirportCode(),
                edge.destinationAirportCode(),
                edge.distanceKm(),
                calculator.fuelLitres(graph, edge),
                calculator.fuelPerPassengerLitres(graph, edge),
                edge.scheduledDepartureUtc(),
                edge.adjustedDepartureUtc(),
                edge.adjustedDepartureDayOffset(),
                edge.delayMinutes(),
                leg.scheduledDepartureInstant(),
                leg.adjustedDepartureInstant(),
                leg.arrivalInstant(),
                FlightDurationCalculator.toMinutes(leg.waitingDuration()),
                FlightDurationCalculator.toMinutes(leg.flightDuration()));
    }
}
