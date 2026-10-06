package com.airportrouteplanner.webapp.route;

import java.time.LocalDateTime;

public record RouteCalculationRequest(
        String originAirportCode,
        String destinationAirportCode,
        RouteType routeType,
        LocalDateTime departureLocalDateTime) {
}
