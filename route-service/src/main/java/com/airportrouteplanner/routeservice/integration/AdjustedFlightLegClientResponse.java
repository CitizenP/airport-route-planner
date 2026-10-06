package com.airportrouteplanner.routeservice.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.airportrouteplanner.routeservice.graph.FlightDirection;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdjustedFlightLegClientResponse(
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
        long delayMinutes) {
}
