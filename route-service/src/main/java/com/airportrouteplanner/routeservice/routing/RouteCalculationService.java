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
    private final FastestRouteCalculator fastestRouteCalculator;

    public RouteCalculationService(
            RouteGraphService routeGraphService,
            StaticRouteCalculator staticRouteCalculator,
            FastestRouteCalculator fastestRouteCalculator) {
        this.routeGraphService = routeGraphService;
        this.staticRouteCalculator = staticRouteCalculator;
        this.fastestRouteCalculator = fastestRouteCalculator;
    }

    public RouteCalculationResponse calculate(RouteCalculationRequest request) {
        if (request == null) {
            throw new InvalidRouteRequestException("request body is required");
        }
        if (request.routeType() == null) {
            throw new InvalidRouteRequestException("routeType is required");
        }
        String origin = normalizeAirportCode(request.originAirportCode(), "originAirportCode");
        String destination = normalizeAirportCode(request.destinationAirportCode(), "destinationAirportCode");
        if (request.routeType() == RouteType.FASTEST && request.departureLocalDateTime() == null) {
            throw new InvalidRouteRequestException("departureLocalDateTime is required for FASTEST routing");
        }

        try {
            RouteGraph graph = routeGraphService.buildGraph();
            if (request.routeType() == RouteType.FASTEST) {
                FastestRouteResult result = fastestRouteCalculator.calculate(
                        graph, origin, destination, request.departureLocalDateTime());
                return RouteCalculationResponse.from(graph, result, staticRouteCalculator);
            }
            StaticRouteResult result = staticRouteCalculator.calculate(
                    graph, origin, destination, request.routeType());
            return RouteCalculationResponse.from(graph, result, staticRouteCalculator);
        } catch (RouteGraphValidationException | InvalidEdgeWeightException | IllegalArgumentException exception) {
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
