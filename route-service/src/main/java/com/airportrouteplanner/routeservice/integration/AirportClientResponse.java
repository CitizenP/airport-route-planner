package com.airportrouteplanner.routeservice.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AirportClientResponse(
        String iataCode,
        double latitude,
        double longitude,
        int utcOffsetMinutes) {
}
