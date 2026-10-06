package com.airportrouteplanner.routeservice.routing;

import java.time.Instant;
import java.util.List;

public record FastestRouteResult(
        String originAirportCode,
        String destinationAirportCode,
        Instant journeyStartUtc,
        Instant journeyArrivalUtc,
        List<TimedRouteLeg> legs,
        double totalDistanceKm,
        double totalFuelLitres,
        double fuelPerPassengerLitres) {

    public FastestRouteResult {
        legs = List.copyOf(legs);
    }
}
