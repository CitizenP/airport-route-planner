package com.airportrouteplanner.flightservice.airport;

public class AirportRunwayCapacityException extends RuntimeException {

    public AirportRunwayCapacityException(String message) {
        super(message);
    }

    public AirportRunwayCapacityException(String message, Throwable cause) {
        super(message, cause);
    }
}
