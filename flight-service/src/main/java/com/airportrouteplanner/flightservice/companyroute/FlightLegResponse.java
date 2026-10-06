package com.airportrouteplanner.flightservice.companyroute;

import java.time.LocalTime;

public record FlightLegResponse(
        Long companyRouteId,
        int routeNumber,
        FlightDirection direction,
        String companyCode,
        Long aircraftTypeId,
        String originAirportCode,
        String destinationAirportCode,
        LocalTime scheduledDepartureUtc) {

    public static FlightLegResponse from(FlightLeg flightLeg) {
        return new FlightLegResponse(
                flightLeg.companyRouteId(),
                flightLeg.routeNumber(),
                flightLeg.direction(),
                flightLeg.companyCode(),
                flightLeg.aircraftTypeId(),
                flightLeg.originAirportCode(),
                flightLeg.destinationAirportCode(),
                flightLeg.scheduledDepartureUtc());
    }
}
