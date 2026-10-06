package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphService;
import com.airportrouteplanner.routeservice.graph.RouteGraphValidationException;
import com.airportrouteplanner.routeservice.summary.RouteGraphUnavailableException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RouteCalculationService {

    private final RouteGraphService routeGraphService;
    private final StaticRouteCalculator staticRouteCalculator;

    public RouteCalculationService(
            RouteGraphService routeGraphService,
            StaticRouteCalculator staticRouteCalculator) {
        this.routeGraphService = routeGraphService;
        this.staticRouteCalculator = staticRouteCalculator;
    }

    public RouteCalculationResponse calculate(RouteCalculationRequest request) {
        if (request == null) {
            throw new InvalidRouteRequestException("request body is required");
        }
        if (request.routeType() == null) {
            throw new InvalidRouteRequestException("routeType is required");
        }
        if (request.routeType() == RouteType.FASTEST) {
            throw new FastestRouteNotImplementedException();
        }
        String origin = normalizeAirportCode(request.originAirportCode(), "originAirportCode");
        String destination = normalizeAirportCode(request.destinationAirportCode(), "destinationAirportCode");

        try {
            RouteGraph graph = routeGraphService.buildGraph();
            StaticRouteResult result = staticRouteCalculator.calculate(
                    graph, origin, destination, request.routeType());
            return RouteCalculationResponse.from(graph, result, staticRouteCalculator);
        } catch (RouteGraphValidationException | InvalidEdgeWeightException exception) {
            throw new RouteGraphUnavailableException(
                    "upstream data failed route-graph validation: " + exception.getMessage(), exception);
        }
    }

    private static String normalizeAirportCode(String airportCode, String fieldName) {
        if (airportCode == null) {
            throw new InvalidRouteRequestException(fieldName + " is required");
        }
        String normalized = airportCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidRouteRequestException(fieldName + " must contain exactly three letters");
        }
        return normalized;
    }
}
