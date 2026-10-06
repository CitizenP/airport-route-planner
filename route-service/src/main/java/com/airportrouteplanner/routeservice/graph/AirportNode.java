package com.airportrouteplanner.routeservice.graph;

public record AirportNode(
        String iataCode,
        double latitude,
        double longitude,
        int utcOffsetMinutes) {
}
