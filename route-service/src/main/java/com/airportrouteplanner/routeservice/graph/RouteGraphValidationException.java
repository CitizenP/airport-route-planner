package com.airportrouteplanner.routeservice.graph;

public class RouteGraphValidationException extends RuntimeException {

    public RouteGraphValidationException(String message) {
        super(message);
    }

    public RouteGraphValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
