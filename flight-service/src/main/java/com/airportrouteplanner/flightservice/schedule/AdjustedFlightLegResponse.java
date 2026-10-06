package com.airportrouteplanner.flightservice.schedule;

import com.airportrouteplanner.flightservice.companyroute.FlightDirection;
import java.time.LocalTime;

public record AdjustedFlightLegResponse(
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

    public static AdjustedFlightLegResponse from(AdjustedFlightLeg flightLeg) {
        return new AdjustedFlightLegResponse(
                flightLeg.companyRouteId(),
                flightLeg.routeNumber(),
                flightLeg.direction(),
                flightLeg.companyCode(),
                flightLeg.aircraftTypeId(),
                flightLeg.originAirportCode(),
                flightLeg.destinationAirportCode(),
                flightLeg.scheduledDepartureUtc(),
                flightLeg.adjustedDepartureUtc(),
                flightLeg.adjustedDepartureDayOffset(),
                flightLeg.delayMinutes());
    }
}
