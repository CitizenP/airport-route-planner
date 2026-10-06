package com.airportrouteplanner.routeservice.routing;

public class FastestRouteNotImplementedException extends RuntimeException {

    public FastestRouteNotImplementedException() {
        super("FASTEST routing is not implemented");
    }
}
