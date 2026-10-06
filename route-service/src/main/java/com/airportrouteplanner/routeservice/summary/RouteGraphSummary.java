package com.airportrouteplanner.routeservice.summary;

public record RouteGraphSummary(
        int airportCount,
        int aircraftTypeCount,
        int companyCount,
        int edgeCount,
        long delayedEdgeCount,
        long maximumDelayMinutes) {
}
