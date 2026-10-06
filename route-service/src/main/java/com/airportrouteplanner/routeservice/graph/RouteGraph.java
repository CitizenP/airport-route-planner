package com.airportrouteplanner.routeservice.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RouteGraph {

    private final Map<String, AirportNode> airports;
    private final Map<Long, AircraftTypeInfo> aircraftTypes;
    private final Set<String> companyCodes;
    private final List<RouteGraphEdge> edges;
    private final Map<String, List<RouteGraphEdge>> outgoingEdges;

    public RouteGraph(
            Map<String, AirportNode> airports,
            Map<Long, AircraftTypeInfo> aircraftTypes,
            Set<String> companyCodes,
            List<RouteGraphEdge> edges,
            Map<String, List<RouteGraphEdge>> outgoingEdges) {
        this.airports = Collections.unmodifiableMap(new LinkedHashMap<>(airports));
        this.aircraftTypes = Collections.unmodifiableMap(new LinkedHashMap<>(aircraftTypes));
        this.companyCodes = Collections.unmodifiableSet(new LinkedHashSet<>(companyCodes));
        this.edges = List.copyOf(edges);

        Map<String, List<RouteGraphEdge>> outgoingCopy = new LinkedHashMap<>();
        outgoingEdges.forEach((airportCode, airportEdges) ->
                outgoingCopy.put(airportCode, List.copyOf(airportEdges)));
        this.outgoingEdges = Collections.unmodifiableMap(outgoingCopy);
    }

    public Map<String, AirportNode> airports() {
        return airports;
    }

    public Map<Long, AircraftTypeInfo> aircraftTypes() {
        return aircraftTypes;
    }

    public Set<String> companyCodes() {
        return companyCodes;
    }

    public List<RouteGraphEdge> edges() {
        return edges;
    }

    public Map<String, List<RouteGraphEdge>> outgoingEdges() {
        return outgoingEdges;
    }
}
