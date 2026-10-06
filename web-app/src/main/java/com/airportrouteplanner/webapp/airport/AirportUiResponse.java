package com.airportrouteplanner.webapp.airport;

public record AirportUiResponse(
        String iataCode,
        String name,
        String city,
        String country,
        double latitude,
        double longitude,
        int numberOfRunways,
        int utcOffsetMinutes,
        String formattedUtcOffset) {

    public static AirportUiResponse from(AirportClientResponse airport, UtcOffsetFormatter formatter) {
        return new AirportUiResponse(
                airport.iataCode(),
                airport.name(),
                airport.city(),
                airport.country(),
                airport.latitude(),
                airport.longitude(),
                airport.numberOfRunways(),
                airport.utcOffsetMinutes(),
                formatter.format(airport.utcOffsetMinutes()));
    }
}
