package com.airportrouteplanner.routeservice.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airportrouteplanner.routeservice.graph.AircraftTypeInfo;
import com.airportrouteplanner.routeservice.graph.AirportNode;
import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DijkstraRouteFinderTests {

    private final DijkstraRouteFinder routeFinder = new DijkstraRouteFinder();

    @Test
    void directEdgeIsSelectedWhenItHasLowestCost() {
        RouteGraphEdge direct = edge(1, "AAA", "DDD", 2);
        RouteGraph graph = graph(Set.of("AAA", "BBB", "DDD"), List.of(
                direct, edge(2, "AAA", "BBB", 2), edge(3, "BBB", "DDD", 2)));

        assertThat(find(graph, "AAA", "DDD").edges()).containsExactly(direct);
    }

    @Test
    void multiLegPathBeatsMoreExpensiveDirectEdge() {
        RouteGraphEdge first = edge(2, "AAA", "BBB", 3);
        RouteGraphEdge second = edge(3, "BBB", "DDD", 3);
        RouteGraph graph = graph(Set.of("AAA", "BBB", "DDD"), List.of(
                edge(1, "AAA", "DDD", 10), first, second));

        DijkstraPath result = find(graph, "AAA", "DDD");
        assertThat(result.edges()).containsExactly(first, second);
        assertThat(result.optimizedCost()).isEqualTo(6);
    }

    @Test
    void directedEdgesAreRespected() {
        RouteGraph graph = graph(Set.of("AAA", "BBB"), List.of(edge(1, "AAA", "BBB", 1)));

        assertThat(routeFinder.findRoute(graph, "BBB", "AAA", RouteGraphEdge::distanceKm)).isEmpty();
    }

    @Test
    void cyclesDoNotBreakTraversal() {
        RouteGraphEdge destination = edge(3, "BBB", "CCC", 1);
        RouteGraph graph = graph(Set.of("AAA", "BBB", "CCC"), List.of(
                edge(1, "AAA", "BBB", 1), edge(2, "BBB", "AAA", 0), destination));

        assertThat(find(graph, "AAA", "CCC").edges().getLast()).isEqualTo(destination);
    }

    @Test
    void unreachableDestinationReturnsEmpty() {
        RouteGraph graph = graph(Set.of("AAA", "BBB", "CCC"), List.of(edge(1, "AAA", "BBB", 1)));

        assertThat(routeFinder.findRoute(graph, "AAA", "CCC", RouteGraphEdge::distanceKm)).isEmpty();
    }

    @Test
    void sameOriginAndDestinationReturnsZeroLegPath() {
        RouteGraph graph = graph(Set.of("AAA"), List.of());

        DijkstraPath result = find(graph, "AAA", "AAA");
        assertThat(result.edges()).isEmpty();
        assertThat(result.optimizedCost()).isZero();
    }

    @Test
    void parallelEdgesRemainUsableAndDeterministic() {
        RouteGraphEdge later = edge(20, "AAA", "BBB", 1);
        RouteGraphEdge earlier = edge(10, "AAA", "BBB", 1);
        RouteGraph graph = graph(Set.of("AAA", "BBB"), List.of(later, earlier));

        assertThat(graph.outgoingEdges().get("AAA")).hasSize(2);
        assertThat(find(graph, "AAA", "BBB").edges()).containsExactly(earlier);
    }

    @Test
    void lowerCostParallelEdgeIsSelected() {
        RouteGraphEdge expensive = edge(1, "AAA", "BBB", 10);
        RouteGraphEdge cheap = edge(2, "AAA", "BBB", 2);
        RouteGraph graph = graph(Set.of("AAA", "BBB"), List.of(expensive, cheap));

        assertThat(find(graph, "AAA", "BBB").edges()).containsExactly(cheap);
    }

    @Test
    void shuffledInputProducesSameEqualCostPath() {
        List<RouteGraphEdge> edges = List.of(
                edge(4, "CCC", "DDD", 1),
                edge(3, "AAA", "CCC", 1),
                edge(2, "BBB", "DDD", 1),
                edge(1, "AAA", "BBB", 1));
        List<RouteGraphEdge> shuffled = new ArrayList<>(edges);
        Collections.shuffle(shuffled, new java.util.Random(42));

        List<Long> first = find(graph(Set.of("AAA", "BBB", "CCC", "DDD"), edges), "AAA", "DDD")
                .edges().stream().map(RouteGraphEdge::companyRouteId).toList();
        List<Long> second = find(graph(Set.of("AAA", "BBB", "CCC", "DDD"), shuffled), "AAA", "DDD")
                .edges().stream().map(RouteGraphEdge::companyRouteId).toList();

        assertThat(first).containsExactly(1L, 2L);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void invalidNegativeAndNonFiniteWeightsFailClearly() {
        RouteGraph graph = graph(Set.of("AAA", "BBB"), List.of(edge(1, "AAA", "BBB", 1)));

        assertThatThrownBy(() -> routeFinder.findRoute(graph, "AAA", "BBB", ignored -> -1))
                .isInstanceOf(InvalidEdgeWeightException.class)
                .hasMessageContaining("finite and non-negative");
        for (double invalidWeight : List.of(Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThatThrownBy(() -> routeFinder.findRoute(graph, "AAA", "BBB", ignored -> invalidWeight))
                    .isInstanceOf(InvalidEdgeWeightException.class)
                    .hasMessageContaining("finite and non-negative");
        }
    }

    private DijkstraPath find(RouteGraph graph, String origin, String destination) {
        Optional<DijkstraPath> result = routeFinder.findRoute(
                graph, origin, destination, RouteGraphEdge::distanceKm);
        return result.orElseThrow();
    }

    private RouteGraph graph(Set<String> airportCodes, List<RouteGraphEdge> edges) {
        Map<String, AirportNode> airports = new LinkedHashMap<>();
        airportCodes.stream().sorted().forEach(code -> airports.put(code, new AirportNode(code, 0, 0, 0)));
        AircraftTypeInfo aircraft = new AircraftTypeInfo(1L, "Maker", "Model", 800, 10000, 10000, 1, 100, 1);
        Map<String, List<RouteGraphEdge>> outgoing = new LinkedHashMap<>();
        airports.keySet().forEach(code -> outgoing.put(code, new ArrayList<>()));
        edges.forEach(edge -> outgoing.get(edge.originAirportCode()).add(edge));
        return new RouteGraph(
                airports,
                Map.of(1L, aircraft),
                new LinkedHashSet<>(Set.of("CO")),
                edges,
                outgoing);
    }

    private RouteGraphEdge edge(long id, String origin, String destination, double distance) {
        return new RouteGraphEdge(
                id,
                1,
                FlightDirection.OUTBOUND,
                "CO",
                1L,
                origin,
                destination,
                LocalTime.of(9, 0),
                LocalTime.of(9, 0),
                0,
                0,
                distance);
    }
}
