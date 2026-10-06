package com.airportrouteplanner.routeservice.routing;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String originAirportCode, String destinationAirportCode) {
        super("no directed route from " + originAirportCode + " to " + destinationAirportCode);
    }
}
