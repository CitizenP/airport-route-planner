package com.airportrouteplanner.webapp.airport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AirportClientResponse(
        String iataCode,
        String name,
        String city,
        String country,
        double latitude,
        double longitude,
        int numberOfRunways,
        int utcOffsetMinutes) {
}
