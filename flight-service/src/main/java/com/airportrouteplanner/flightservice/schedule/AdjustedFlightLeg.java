package com.airportrouteplanner.flightservice.schedule;

import com.airportrouteplanner.flightservice.companyroute.FlightDirection;
import java.time.LocalTime;

public record AdjustedFlightLeg(
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
