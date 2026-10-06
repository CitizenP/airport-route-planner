package com.airportrouteplanner.airportservice.airport;

public record AirportResponse(
        String iataCode,
        String name,
        String city,
        String country,
        double latitude,
        double longitude,
        int numberOfRunways,
        int utcOffsetMinutes) {

    public static AirportResponse from(Airport airport) {
        return new AirportResponse(
                airport.getIataCode(),
                airport.getName(),
                airport.getCity(),
                airport.getCountry(),
                airport.getLatitude(),
                airport.getLongitude(),
                airport.getNumberOfRunways(),
                airport.getUtcOffsetMinutes());
    }
}
