package com.airportrouteplanner.routeservice.graph;

import java.time.LocalTime;

public record RouteGraphEdge(
        Long companyRouteId,
        int routeNumber,
        FlightDirection direction,
        String companyCode,
        Long aircraftTypeId,
        String originAirportCode,
        String destinationAirportCode,
        LocalTime scheduledDepartureUtc,
        LocalTime adjustedDepartureUtc,
        int adjustedDepartureDayOffset,
        long delayMinutes,
        double distanceKm) {
}
