package com.airportrouteplanner.routeservice.summary;

import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphBuilder;
import com.airportrouteplanner.routeservice.graph.RouteGraphValidationException;
import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegDataProvider;
import com.airportrouteplanner.routeservice.integration.AircraftTypeDataProvider;
import com.airportrouteplanner.routeservice.integration.AirportDataProvider;
import com.airportrouteplanner.routeservice.integration.FlightCompanyDataProvider;
import org.springframework.stereotype.Service;

@Service
public class RouteGraphSummaryService {

    private final AirportDataProvider airportDataProvider;
    private final AircraftTypeDataProvider aircraftTypeDataProvider;
    private final FlightCompanyDataProvider flightCompanyDataProvider;
    private final AdjustedFlightLegDataProvider flightLegDataProvider;
    private final RouteGraphBuilder routeGraphBuilder;

    public RouteGraphSummaryService(
            AirportDataProvider airportDataProvider,
            AircraftTypeDataProvider aircraftTypeDataProvider,
            FlightCompanyDataProvider flightCompanyDataProvider,
            AdjustedFlightLegDataProvider flightLegDataProvider,
            RouteGraphBuilder routeGraphBuilder) {
        this.airportDataProvider = airportDataProvider;
        this.aircraftTypeDataProvider = aircraftTypeDataProvider;
        this.flightCompanyDataProvider = flightCompanyDataProvider;
        this.flightLegDataProvider = flightLegDataProvider;
        this.routeGraphBuilder = routeGraphBuilder;
    }

    public RouteGraphSummary getSummary() {
        RouteGraph graph;
        try {
            graph = routeGraphBuilder.build(
                    airportDataProvider.getAirports(),
                    aircraftTypeDataProvider.getAircraftTypes(),
                    flightCompanyDataProvider.getFlightCompanies(),
                    flightLegDataProvider.getAdjustedFlightLegs());
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
