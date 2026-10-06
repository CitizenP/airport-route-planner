package com.airportrouteplanner.webapp.route;

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
}
