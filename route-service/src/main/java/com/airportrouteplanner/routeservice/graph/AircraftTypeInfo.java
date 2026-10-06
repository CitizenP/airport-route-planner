package com.airportrouteplanner.routeservice.graph;

public record AircraftTypeInfo(
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
