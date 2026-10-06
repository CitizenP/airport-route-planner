package com.airportrouteplanner.routeservice.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AircraftTypeClientResponse(
        Long id,
        String manufacturer,
        String model,
        double cruiseSpeedKmH,
        double maximumRangeKm,
        double fuelCapacityLitres,
        double fuelConsumptionLitresPerKm,
        int passengerCapacity,
        double fuelConsumptionPerPassenger) {
}
