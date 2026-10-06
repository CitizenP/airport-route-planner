package com.airportrouteplanner.webapp.route;

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
}
