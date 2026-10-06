package com.airportrouteplanner.webapp.airport;

public class AirportServiceUnavailableException extends RuntimeException {

    public AirportServiceUnavailableException(String message) {
        super(message);
    }

    public AirportServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
