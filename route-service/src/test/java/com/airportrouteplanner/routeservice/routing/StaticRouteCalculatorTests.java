package com.airportrouteplanner.routeservice.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.airportrouteplanner.routeservice.graph.AircraftTypeInfo;
import com.airportrouteplanner.routeservice.graph.AirportNode;
import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StaticRouteCalculatorTests {

    private final StaticRouteCalculator calculator = new StaticRouteCalculator(new DijkstraRouteFinder());

    @Test
    void shortestUsesDistanceAndIgnoresAircraftFuelCharacteristics() {
        RouteGraphEdge direct = edge(1, "AAA", "DDD", 10, 1);
        RouteGraph graph = graph(
                List.of(direct, edge(2, "AAA", "BBB", 6, 2), edge(3, "BBB", "DDD", 6, 2)),
                Map.of(1L, aircraft(1, 100, 100), 2L, aircraft(2, 1, 1)));

        StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", RouteType.SHORTEST);

        assertThat(result.edges()).containsExactly(direct);
        assertThat(result.optimizedCost()).isEqualTo(10);
        assertThat(result.totalDistanceKm()).isEqualTo(10);
        assertThat(result.totalFuelLitres()).isEqualTo(1000);
        assertThat(result.fuelPerPassengerLitres()).isEqualTo(10);
    }

    @Test
    void geographicallyShorterMultiLegPathBeatsLongerDirectEdge() {
        RouteGraphEdge first = edge(2, "AAA", "BBB", 5, 1);
        RouteGraphEdge second = edge(3, "BBB", "DDD", 5, 1);
        RouteGraph graph = graph(
                List.of(edge(1, "AAA", "DDD", 15, 1), first, second),
                Map.of(1L, aircraft(1, 2, 3)));

        StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", RouteType.SHORTEST);

        assertThat(result.edges()).containsExactly(first, second);
        assertThat(result.totalDistanceKm()).isEqualTo(
                result.edges().stream().mapToDouble(RouteGraphEdge::distanceKm).sum());
    }

    @Test
    void cheapestUsesExactWholeAircraftFuelFormulaAndReportsAllTotals() {
        RouteGraph graph = graph(
                List.of(edge(1, "AAA", "DDD", 10, 1)),
                Map.of(1L, aircraft(1, 3, 7)));

        StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", RouteType.CHEAPEST);

        assertThat(result.optimizedCost()).isEqualTo(10 * 3);
        assertThat(result.totalDistanceKm()).isEqualTo(10);
        assertThat(result.totalFuelLitres()).isEqualTo(30);
        assertThat(result.fuelPerPassengerLitres()).isEqualTo(10.0 / 100.0 * 7);
    }

    @Test
    void longerRouteCanWinCheapestWithMoreEfficientAircraft() {
        RouteGraphEdge first = edge(2, "AAA", "BBB", 8, 2);
        RouteGraphEdge second = edge(3, "BBB", "DDD", 8, 2);
        RouteGraph graph = graph(
                List.of(edge(1, "AAA", "DDD", 10, 1), first, second),
                Map.of(1L, aircraft(1, 10, 1), 2L, aircraft(2, 1, 10)));

        StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", RouteType.CHEAPEST);

        assertThat(result.edges()).containsExactly(first, second);
        assertThat(result.totalDistanceKm()).isEqualTo(16);
        assertThat(result.totalFuelLitres()).isEqualTo(16);
    }

    @Test
    void passengerCapacityDoesNotInfluenceCheapest() {
        RouteGraphEdge deterministicFirst = edge(1, "AAA", "DDD", 10, 1);
        RouteGraphEdge second = edge(2, "AAA", "DDD", 10, 2);
        AircraftTypeInfo lowCapacity = aircraft(1, 2, 5, 1);
        AircraftTypeInfo highCapacity = aircraft(2, 2, 5, 1000);
        RouteGraph graph = graph(
                List.of(second, deterministicFirst),
                Map.of(1L, lowCapacity, 2L, highCapacity));

        assertThat(calculator.calculate(graph, "AAA", "DDD", RouteType.CHEAPEST).edges())
                .containsExactly(deterministicFirst);
    }

    @Test
    void ecologicalUsesExactPerPassengerFormula() {
        RouteGraph graph = graph(
                List.of(edge(1, "AAA", "DDD", 250, 1)),
                Map.of(1L, aircraft(1, 4, 2.5)));

        StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", RouteType.ECOLOGICAL);

        assertThat(result.optimizedCost()).isEqualTo(250.0 / 100.0 * 2.5);
        assertThat(result.fuelPerPassengerLitres()).isEqualTo(6.25);
        assertThat(result.totalFuelLitres()).isEqualTo(1000);
    }

    @Test
    void higherWholeAircraftFuelRouteCanWinEcological() {
        RouteGraphEdge first = edge(2, "AAA", "BBB", 10, 2);
        RouteGraphEdge second = edge(3, "BBB", "DDD", 10, 2);
        RouteGraph graph = graph(
                List.of(edge(1, "AAA", "DDD", 10, 1), first, second),
                Map.of(1L, aircraft(1, 1, 10), 2L, aircraft(2, 2, 1)));

        StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", RouteType.ECOLOGICAL);

        assertThat(result.edges()).containsExactly(first, second);
        assertThat(result.totalFuelLitres()).isEqualTo(40);
        assertThat(result.fuelPerPassengerLitres()).isEqualTo(0.2);
    }

    @Test
    void allThreeMetricsSelectDifferentPaths() {
        RouteGraphEdge shortest = edge(1, "AAA", "DDD", 10, 1);
        RouteGraphEdge cheapFirst = edge(2, "AAA", "CCC", 10, 2);
        RouteGraphEdge cheapSecond = edge(3, "CCC", "DDD", 10, 2);
        RouteGraphEdge ecoFirst = edge(4, "AAA", "EEE", 15, 3);
        RouteGraphEdge ecoSecond = edge(5, "EEE", "DDD", 15, 3);
        RouteGraph graph = graph(
                List.of(shortest, cheapFirst, cheapSecond, ecoFirst, ecoSecond),
                Map.of(
                        1L, aircraft(1, 10, 10),
                        2L, aircraft(2, 1, 10),
                        3L, aircraft(3, 2, 1)));

        assertThat(calculator.calculate(graph, "AAA", "DDD", RouteType.SHORTEST).edges())
                .containsExactly(shortest);
        assertThat(calculator.calculate(graph, "AAA", "DDD", RouteType.CHEAPEST).edges())
                .containsExactly(cheapFirst, cheapSecond);
        assertThat(calculator.calculate(graph, "AAA", "DDD", RouteType.ECOLOGICAL).edges())
                .containsExactly(ecoFirst, ecoSecond);
    }

    @Test
    void publicResponseTotalsEqualTheSelectedLegTotalsForEveryStaticMetric() {
        RouteGraph graph = graph(
                List.of(
                        edge(1, "AAA", "BBB", 4, 1),
                        edge(2, "BBB", "DDD", 6, 2)),
                Map.of(1L, aircraft(1, 2, 5), 2L, aircraft(2, 3, 7)));

        for (RouteType routeType : List.of(RouteType.SHORTEST, RouteType.CHEAPEST, RouteType.ECOLOGICAL)) {
            StaticRouteResult result = calculator.calculate(graph, "AAA", "DDD", routeType);
            RouteCalculationResponse response = RouteCalculationResponse.from(graph, result, calculator);

            assertThat(response.totalDistanceKm())
                    .isEqualTo(response.legs().stream().mapToDouble(RouteLegResponse::distanceKm).sum());
            assertThat(response.totalFuelLitres())
                    .isEqualTo(response.legs().stream().mapToDouble(RouteLegResponse::fuelLitres).sum());
            assertThat(response.fuelPerPassengerLitres())
                    .isEqualTo(response.legs().stream()
                            .mapToDouble(RouteLegResponse::fuelPerPassengerLitres)
                            .sum());
            assertThat(response.totalJourneyMinutes()).isNull();
        }
    }

    private RouteGraph graph(List<RouteGraphEdge> edges, Map<Long, AircraftTypeInfo> aircraftTypes) {
        Set<String> airportCodes = new LinkedHashSet<>();
        edges.forEach(edge -> {
            airportCodes.add(edge.originAirportCode());
            airportCodes.add(edge.destinationAirportCode());
        });
        Map<String, AirportNode> airports = new LinkedHashMap<>();
        airportCodes.forEach(code -> airports.put(code, new AirportNode(code, 0, 0, 0)));
        Map<String, List<RouteGraphEdge>> outgoing = new LinkedHashMap<>();
        airportCodes.forEach(code -> outgoing.put(code, new ArrayList<>()));
        edges.forEach(edge -> outgoing.get(edge.originAirportCode()).add(edge));
        return new RouteGraph(airports, aircraftTypes, Set.of("CO"), edges, outgoing);
    }

    private AircraftTypeInfo aircraft(long id, double fuelPerKm, double fuelPerPassenger) {
        return aircraft(id, fuelPerKm, fuelPerPassenger, 100);
    }

    private AircraftTypeInfo aircraft(
            long id,
            double fuelPerKm,
            double fuelPerPassenger,
            int passengerCapacity) {
        return new AircraftTypeInfo(
                id, "Maker", "Model" + id, 800, 10000, 10000,
                fuelPerKm, passengerCapacity, fuelPerPassenger);
    }

    private RouteGraphEdge edge(
            long id,
            String origin,
            String destination,
            double distance,
            long aircraftTypeId) {
        return new RouteGraphEdge(
                id,
                1,
                FlightDirection.OUTBOUND,
                "CO",
                aircraftTypeId,
                origin,
                destination,
                LocalTime.of(9, 0),
                LocalTime.of(9, 0),
                0,
                0,
                distance);
    }
}
