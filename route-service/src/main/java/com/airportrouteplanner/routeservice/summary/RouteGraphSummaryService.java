package com.airportrouteplanner.routeservice.summary;

import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphService;
import com.airportrouteplanner.routeservice.graph.RouteGraphValidationException;
import org.springframework.stereotype.Service;

@Service
public class RouteGraphSummaryService {

    private final RouteGraphService routeGraphService;

    public RouteGraphSummaryService(RouteGraphService routeGraphService) {
        this.routeGraphService = routeGraphService;
    }

    public RouteGraphSummary getSummary() {
        RouteGraph graph;
        try {
            graph = routeGraphService.buildGraph();
        } catch (RouteGraphValidationException exception) {
            throw new RouteGraphUnavailableException(
                    "upstream data failed route-graph validation: " + exception.getMessage(), exception);
        }

        long delayedEdges = graph.edges().stream().filter(edge -> edge.delayMinutes() > 0).count();
        long maximumDelay = graph.edges().stream()
                .mapToLong(edge -> edge.delayMinutes())
                .max()
                .orElse(0);
        return new RouteGraphSummary(
                graph.airports().size(),
                graph.aircraftTypes().size(),
                graph.companyCodes().size(),
                graph.edges().size(),
                delayedEdges,
                maximumDelay);
    }
}
