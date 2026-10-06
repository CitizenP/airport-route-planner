package com.airportrouteplanner.routeservice.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.HaversineDistanceCalculator;
import com.airportrouteplanner.routeservice.graph.RouteGraphBuilder;
import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegClientResponse;
import com.airportrouteplanner.routeservice.integration.AircraftTypeClientResponse;
import com.airportrouteplanner.routeservice.integration.AirportClientResponse;
import com.airportrouteplanner.routeservice.integration.FlightCompanyClientResponse;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteGraphSummaryServiceTests {

    @Test
    void calculatesGraphSummary() {
        List<AirportClientResponse> airports = List.of(
                new AirportClientResponse("AAA", 0, 0, 0),
                new AirportClientResponse("BBB", 0, 1, 60));
        List<AircraftTypeClientResponse> aircraft = List.of(
                new AircraftTypeClientResponse(1L, "Maker", "Model", 800, 1000, 10000, 3, 100, 3));
        List<FlightCompanyClientResponse> companies = List.of(new FlightCompanyClientResponse("CO"));
        List<AdjustedFlightLegClientResponse> legs = List.of(
                leg(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0),
                leg(1, FlightDirection.RETURN, "BBB", "AAA", 5));

        RouteGraphSummaryService service = new RouteGraphSummaryService(
                () -> airports,
                () -> aircraft,
                () -> companies,
                () -> legs,
                new RouteGraphBuilder(new HaversineDistanceCalculator()));

        assertThat(service.getSummary()).isEqualTo(new RouteGraphSummary(2, 1, 1, 2, 1, 5));
    }

    private AdjustedFlightLegClientResponse leg(
            long routeId,
            FlightDirection direction,
            String origin,
            String destination,
            long delayMinutes) {
        return new AdjustedFlightLegClientResponse(
                routeId,
                1,
                direction,
                "CO",
                1L,
                origin,
                destination,
                LocalTime.of(9, 0),
                LocalTime.of(9, 0).plusMinutes(delayMinutes),
                0,
                delayMinutes);
    }
}
