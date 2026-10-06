package com.airportrouteplanner.fleetservice.aircrafttype;

public record AircraftTypeResponse(
        Long id,
        String manufacturer,
        String model,
        double cruiseSpeedKmH,
        double maximumRangeKm,
        double fuelCapacityLitres,
        double fuelConsumptionLitresPerKm,
        int passengerCapacity,
        double fuelConsumptionPerPassenger) {

    public static AircraftTypeResponse from(AircraftType aircraftType) {
        return new AircraftTypeResponse(
                aircraftType.getId(),
                aircraftType.getManufacturer(),
                aircraftType.getModel(),
                aircraftType.getCruiseSpeedKmH(),
                aircraftType.getMaximumRangeKm(),
                aircraftType.getFuelCapacityLitres(),
                aircraftType.getFuelConsumptionLitresPerKm(),
                aircraftType.getPassengerCapacity(),
                aircraftType.getFuelConsumptionPerPassenger());
    }
}
