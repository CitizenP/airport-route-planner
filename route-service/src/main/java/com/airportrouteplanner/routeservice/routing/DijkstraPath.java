package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.util.List;

public record DijkstraPath(List<RouteGraphEdge> edges, double optimizedCost) {

    public DijkstraPath {
        edges = List.copyOf(edges);
    }
}
