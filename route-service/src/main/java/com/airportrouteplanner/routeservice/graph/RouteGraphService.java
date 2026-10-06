package com.airportrouteplanner.routeservice.graph;

import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegDataProvider;
import com.airportrouteplanner.routeservice.integration.AircraftTypeDataProvider;
import com.airportrouteplanner.routeservice.integration.AirportDataProvider;
import com.airportrouteplanner.routeservice.integration.FlightCompanyDataProvider;
import org.springframework.stereotype.Service;

@Service
public class RouteGraphService {

    private final AirportDataProvider airportDataProvider;
    private final AircraftTypeDataProvider aircraftTypeDataProvider;
    private final FlightCompanyDataProvider flightCompanyDataProvider;
    private final AdjustedFlightLegDataProvider flightLegDataProvider;
    private final RouteGraphBuilder routeGraphBuilder;

    public RouteGraphService(
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

    public RouteGraph buildGraph() {
        return routeGraphBuilder.build(
                airportDataProvider.getAirports(),
                aircraftTypeDataProvider.getAircraftTypes(),
                flightCompanyDataProvider.getFlightCompanies(),
                flightLegDataProvider.getAdjustedFlightLegs());
    }
}
