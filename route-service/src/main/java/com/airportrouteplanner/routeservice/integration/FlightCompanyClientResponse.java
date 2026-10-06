package com.airportrouteplanner.routeservice.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightCompanyClientResponse(String code) {
}
