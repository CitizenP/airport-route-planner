package com.airportrouteplanner.flightservice.airport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AirportRunwayResponse(String iataCode, int numberOfRunways) {
}
