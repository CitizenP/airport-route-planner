package com.airportrouteplanner.routeservice.routing;

public class AirportNotFoundException extends RuntimeException {

    public AirportNotFoundException(String airportCode) {
        super("unknown airport code " + airportCode);
    }
}
