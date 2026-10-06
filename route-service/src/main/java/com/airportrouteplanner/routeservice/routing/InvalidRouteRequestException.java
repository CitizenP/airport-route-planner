package com.airportrouteplanner.routeservice.routing;

public class InvalidRouteRequestException extends RuntimeException {

    public InvalidRouteRequestException(String message) {
        super(message);
    }
}
