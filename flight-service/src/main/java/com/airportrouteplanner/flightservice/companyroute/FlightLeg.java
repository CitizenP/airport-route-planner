package com.airportrouteplanner.flightservice.companyroute;

import java.time.LocalTime;

public record FlightLeg(
        Long companyRouteId,
        int routeNumber,
        FlightDirection direction,
        String companyCode,
        Long aircraftTypeId,
        String originAirportCode,
        String destinationAirportCode,
        LocalTime scheduledDepartureUtc) {
}
