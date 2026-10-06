package com.airportrouteplanner.routeservice.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegClientResponse;
import com.airportrouteplanner.routeservice.integration.AircraftTypeClientResponse;
import com.airportrouteplanner.routeservice.integration.AirportClientResponse;
import com.airportrouteplanner.routeservice.integration.FlightCompanyClientResponse;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteGraphBuilderTests {

    private final RouteGraphBuilder builder = new RouteGraphBuilder(new HaversineDistanceCalculator());

    @Test
    void buildsDirectedGraphEdgeAndAircraftCatalogue() {
        RouteGraph graph = build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0)));

        RouteGraphEdge edge = graph.edges().getFirst();
        assertThat(edge.originAirportCode()).isEqualTo("AAA");
        assertThat(edge.destinationAirportCode()).isEqualTo("BBB");
        assertThat(edge.distanceKm()).isCloseTo(111.195, org.assertj.core.data.Offset.offset(0.001));
        assertThat(graph.outgoingEdges().get("AAA")).containsExactly(edge);
        assertThat(graph.outgoingEdges().get("BBB")).isEmpty();
        assertThat(graph.aircraftTypes()).containsKey(1L);
        assertThat(graph.companyCodes()).containsExactly("CO");
    }

    @Test
    void preservesParallelEdgesBetweenSameAirports() {
        RouteGraph graph = build(List.of(
                edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0),
                edge(2, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0),
                edge(3, FlightDirection.RETURN, "AAA", "BBB", 0, 0)));

        assertThat(graph.edges()).hasSize(3);
        assertThat(graph.outgoingEdges().get("AAA")).hasSize(3);
    }

    @Test
    void rejectsMissingOriginAirport() {
        assertThatThrownBy(() -> build(List.of(edge(1, FlightDirection.OUTBOUND, "ZZZ", "BBB", 0, 0))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("missing origin airport ZZZ");
    }

    @Test
    void rejectsMissingDestinationAirport() {
        assertThatThrownBy(() -> build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "ZZZ", 0, 0))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("missing destination airport ZZZ");
    }

    @Test
    void rejectsMissingAircraftType() {
        AdjustedFlightLegClientResponse edge = edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0);
        edge = new AdjustedFlightLegClientResponse(
                edge.companyRouteId(), edge.routeNumber(), edge.direction(), edge.companyCode(), 99L,
                edge.originAirportCode(), edge.destinationAirportCode(), edge.scheduledDepartureUtc(),
                edge.adjustedDepartureUtc(), edge.adjustedDepartureDayOffset(), edge.delayMinutes());

        AdjustedFlightLegClientResponse missingAircraftEdge = edge;
        assertThatThrownBy(() -> build(List.of(missingAircraftEdge)))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("missing aircraft type 99");
    }

    @Test
    void rejectsMissingCompany() {
        AdjustedFlightLegClientResponse edge = edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0);
        edge = new AdjustedFlightLegClientResponse(
                edge.companyRouteId(), edge.routeNumber(), edge.direction(), "XX", edge.aircraftTypeId(),
                edge.originAirportCode(), edge.destinationAirportCode(), edge.scheduledDepartureUtc(),
                edge.adjustedDepartureUtc(), edge.adjustedDepartureDayOffset(), edge.delayMinutes());

        AdjustedFlightLegClientResponse missingCompanyEdge = edge;
        assertThatThrownBy(() -> build(List.of(missingCompanyEdge)))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("missing flight company XX");
    }

    @Test
    void rejectsDuplicateAirportCodes() {
        List<AirportClientResponse> airports = List.of(
                airport("AAA", 0, 0), airport("AAA", 1, 1), airport("BBB", 0, 1));

        assertThatThrownBy(() -> builder.build(airports, aircraftTypes(), companies(), List.of()))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("duplicate airport IATA code AAA");
    }

    @Test
    void rejectsDuplicateAircraftIds() {
        List<AircraftTypeClientResponse> aircraftTypes = List.of(aircraft(1, 1000), aircraft(1, 2000));

        assertThatThrownBy(() -> builder.build(airports(), aircraftTypes, companies(), List.of()))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("duplicate aircraft type ID 1");
    }

    @Test
    void rejectsInvalidAircraftFuelValues() {
        AircraftTypeClientResponse invalidPerKm = new AircraftTypeClientResponse(
                1L, "Maker", "Model", 800, 1000, 10000, 0, 100, 3);
        AircraftTypeClientResponse invalidPerPassenger = new AircraftTypeClientResponse(
                1L, "Maker", "Model", 800, 1000, 10000, 3, 100, Double.NaN);

        assertThatThrownBy(() -> builder.build(airports(), List.of(invalidPerKm), companies(), List.of()))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("fuel consumption per kilometre");
        assertThatThrownBy(() -> builder.build(
                        airports(), List.of(invalidPerPassenger), companies(), List.of()))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("fuel consumption per passenger");
    }

    @Test
    void rejectsNonPositiveAndNonFiniteCruiseSpeeds() {
        for (double speed : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            AircraftTypeClientResponse invalid = new AircraftTypeClientResponse(
                    1L, "Maker", "Model", speed, 1000, 10000, 3, 100, 3);

            assertThatThrownBy(() -> builder.build(airports(), List.of(invalid), companies(), List.of()))
                    .isInstanceOf(RouteGraphValidationException.class)
                    .hasMessageContaining("invalid cruise speed");
        }
    }

    @Test
    void rejectsUtcOffsetOutsideJavaZoneOffsetRange() {
        List<AirportClientResponse> airports = List.of(
                new AirportClientResponse("AAA", 0, 0, 18 * 60 + 1),
                airport("BBB", 0, 1));

        assertThatThrownBy(() -> builder.build(airports, aircraftTypes(), companies(), List.of()))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("invalid UTC offset for airport AAA");
    }

    @Test
    void rejectsInconsistentAdjustedScheduleMetadata() {
        AdjustedFlightLegClientResponse inconsistent = new AdjustedFlightLegClientResponse(
                1L, 1, FlightDirection.OUTBOUND, "CO", 1L, "AAA", "BBB",
                LocalTime.of(14, 0), LocalTime.of(14, 5), 0, 10);

        assertThatThrownBy(() -> build(List.of(inconsistent)))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("inconsistent adjusted schedule metadata");
    }

    @Test
    void rejectsDuplicateCompanyCodes() {
        List<FlightCompanyClientResponse> companies = List.of(company("CO"), company("CO"));

        assertThatThrownBy(() -> builder.build(airports(), aircraftTypes(), companies, List.of()))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("duplicate flight-company code CO");
    }

    @Test
    void rejectsDuplicateLogicalFlightLegIdentity() {
        assertThatThrownBy(() -> build(List.of(
                        edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0),
                        edge(1, FlightDirection.OUTBOUND, "BBB", "AAA", 0, 0))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessage("duplicate flight-leg identity 1/OUTBOUND");
    }

    @Test
    void rejectsAircraftRangeViolation() {
        assertThatThrownBy(() -> builder.build(
                        airports(),
                        List.of(aircraft(1, 100)),
                        companies(),
                        List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("distance exceeds aircraft type 1 range");
    }

    @Test
    void exposesOnlyImmutableCompletedGraphCollections() {
        RouteGraph graph = build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, 0)));

        assertThatThrownBy(() -> graph.airports().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.aircraftTypes().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.companyCodes().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.edges().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.outgoingEdges().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.outgoingEdges().get("AAA").clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preservesDelayedEdgeMetadata() {
        RouteGraph graph = build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 1, 5)));

        RouteGraphEdge edge = graph.edges().getFirst();
        assertThat(edge.adjustedDepartureUtc()).isEqualTo(LocalTime.of(0, 0));
        assertThat(edge.adjustedDepartureDayOffset()).isEqualTo(1);
        assertThat(edge.delayMinutes()).isEqualTo(5);
    }

    @Test
    void rejectsNegativeDelayAndDayOffset() {
        assertThatThrownBy(() -> build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0, -1))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("delayMinutes");
        assertThatThrownBy(() -> build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "BBB", -1, 0))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("adjustedDepartureDayOffset");
    }

    @Test
    void rejectsIdenticalOriginAndDestination() {
        assertThatThrownBy(() -> build(List.of(edge(1, FlightDirection.OUTBOUND, "AAA", "AAA", 0, 0))))
                .isInstanceOf(RouteGraphValidationException.class)
                .hasMessageContaining("identical origin and destination");
    }

    private RouteGraph build(List<AdjustedFlightLegClientResponse> edges) {
        return builder.build(airports(), aircraftTypes(), companies(), edges);
    }

    private List<AirportClientResponse> airports() {
        return List.of(airport("AAA", 0, 0), airport("BBB", 0, 1));
    }

    private AirportClientResponse airport(String code, double latitude, double longitude) {
        return new AirportClientResponse(code, latitude, longitude, 0);
    }

    private List<AircraftTypeClientResponse> aircraftTypes() {
        return List.of(aircraft(1, 1000));
    }

    private AircraftTypeClientResponse aircraft(long id, double rangeKm) {
        return new AircraftTypeClientResponse(id, "Maker", "Model", 800, rangeKm, 10000, 3, 100, 3);
    }

    private List<FlightCompanyClientResponse> companies() {
        return List.of(company("CO"));
    }

    private FlightCompanyClientResponse company(String code) {
        return new FlightCompanyClientResponse(code);
    }

    private AdjustedFlightLegClientResponse edge(
            long routeId,
            FlightDirection direction,
            String origin,
            String destination,
            int dayOffset,
            long delayMinutes) {
        LocalTime scheduled = LocalTime.of(23, 55);
        LocalTime adjusted = scheduled.plusMinutes(delayMinutes);
        return new AdjustedFlightLegClientResponse(
                routeId,
                1,
                direction,
                "CO",
                1L,
                origin,
                destination,
                scheduled,
                adjusted,
                dayOffset,
                delayMinutes);
    }
}
