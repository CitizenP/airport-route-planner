package com.airportrouteplanner.routeservice.routing;

import java.time.LocalDateTime;

public record RouteCalculationRequest(
        String originAirportCode,
        String destinationAirportCode,
        RouteType routeType,
        LocalDateTime departureLocalDateTime) {
}
