package com.airportrouteplanner.routeservice.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.airportrouteplanner.routeservice.graph.AircraftTypeInfo;
import com.airportrouteplanner.routeservice.graph.AirportNode;
import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EarliestArrivalRouteFinderTests {

    private final EarliestArrivalRouteFinder finder = new EarliestArrivalRouteFinder(
            new DailyFlightSchedule(), new FlightDurationCalculator());

    @Test
    void findsDirectFlightAndIncludesInitialWaitingTime() {
        RouteGraphEdge direct = edge(1, "CO", "AAA", "BBB", "09:00", "09:00", 100, 1);

        EarliestArrivalPath path = find(graph(List.of(direct), aircraft(1, 100)), "AAA", "BBB", "08:30");

        assertThat(path.legs()).extracting(leg -> leg.edge().companyRouteId()).containsExactly(1L);
        assertThat(path.legs().getFirst().waitingDuration()).isEqualTo(java.time.Duration.ofMinutes(30));
        assertThat(path.journeyArrivalUtc()).isEqualTo(instant("10:00"));
    }

    @Test
    void includesConnectionWaitingTime() {
        RouteGraph graph = graph(
                List.of(
                        edge(1, "CO", "AAA", "BBB", "09:00", "09:00", 100, 1),
                        edge(2, "CO", "BBB", "CCC", "11:00", "11:00", 100, 1)),
                aircraft(1, 100));

        EarliestArrivalPath path = find(graph, "AAA", "CCC", "08:00");

        assertThat(path.legs().get(1).waitingDuration()).isEqualTo(java.time.Duration.ofHours(1));
        assertThat(path.journeyArrivalUtc()).isEqualTo(instant("12:00"));
    }

    @Test
    void missedConnectionUsesNextDailyOccurrence() {
        RouteGraph graph = graph(
                List.of(
                        edge(1, "CO", "AAA", "BBB", "09:00", "09:00", 120, 1),
                        edge(2, "CO", "BBB", "CCC", "10:00", "10:00", 100, 1)),
                aircraft(1, 100));

        EarliestArrivalPath path = find(graph, "AAA", "CCC", "08:00");

        assertThat(path.legs().get(1).adjustedDepartureInstant())
                .isEqualTo(Instant.parse("2026-10-21T10:00:00Z"));
    }

    @Test
    void exactTimeConnectionIsAllowed() {
        RouteGraph graph = graph(
                List.of(
                        edge(1, "CO", "AAA", "BBB", "09:00", "09:00", 100, 1),
                        edge(2, "CO", "BBB", "CCC", "10:00", "10:00", 100, 1)),
                aircraft(1, 100));

        EarliestArrivalPath path = find(graph, "AAA", "CCC", "08:00");

        assertThat(path.legs().get(1).waitingDuration()).isZero();
        assertThat(path.legs().get(1).adjustedDepartureInstant()).isEqualTo(instant("10:00"));
    }

    @Test
    void longerGeographicalPathCanArriveEarlier() {
        RouteGraphEdge first = edge(2, "CO", "AAA", "BBB", "08:00", "08:00", 100, 2);
        RouteGraphEdge second = edge(3, "CO", "BBB", "CCC", "08:06", "08:06", 100, 2);
        RouteGraph graph = graph(
                List.of(edge(1, "CO", "AAA", "CCC", "08:00", "08:00", 100, 1), first, second),
                aircraft(1, 100), aircraft(2, 1000));

        assertThat(find(graph, "AAA", "CCC", "08:00").legs())
                .extracting(leg -> leg.edge().companyRouteId())
                .containsExactly(2L, 3L);
    }

    @Test
    void moreFlightTimeCanWinBecauseItsScheduleLeavesToday() {
        RouteGraphEdge first = edge(2, "CO", "AAA", "BBB", "08:00", "08:00", 100, 1);
        RouteGraphEdge second = edge(3, "CO", "BBB", "CCC", "09:00", "09:00", 100, 1);
        RouteGraph graph = graph(
                List.of(edge(1, "CO", "AAA", "CCC", "07:00", "07:00", 10, 1), first, second),
                aircraft(1, 100));

        assertThat(find(graph, "AAA", "CCC", "07:30").legs())
                .extracting(leg -> leg.edge().companyRouteId())
                .containsExactly(2L, 3L);
    }

    @Test
    void adjustedDepartureRatherThanMissedScheduledTimeControlsAvailability() {
        RouteGraphEdge delayed = edge(1, "CO", "AAA", "BBB", "14:00", "14:05", 100, 1, 5);

        TimedRouteLeg leg = find(graph(List.of(delayed), aircraft(1, 100)), "AAA", "BBB", "14:01")
                .legs().getFirst();

        assertThat(leg.scheduledDepartureInstant()).isEqualTo(instant("14:00"));
        assertThat(leg.adjustedDepartureInstant()).isEqualTo(instant("14:05"));
    }

    @Test
    void slowerDirectFlightCanLoseToConnection() {
        RouteGraphEdge first = edge(2, "CO", "AAA", "BBB", "08:00", "08:00", 100, 2);
        RouteGraphEdge second = edge(3, "CO", "BBB", "CCC", "08:06", "08:06", 100, 2);
        RouteGraph graph = graph(
                List.of(edge(1, "CO", "AAA", "CCC", "08:00", "08:00", 100, 1), first, second),
                aircraft(1, 50), aircraft(2, 1000));

        assertThat(find(graph, "AAA", "CCC", "08:00").legs()).hasSize(2);
    }

    @Test
    void fasterAircraftWinsForSameDepartureAndDistanceWithoutWholeMinuteRounding() {
        RouteGraphEdge slower = edge(1, "AA", "AAA", "BBB", "08:00", "08:00", 100, 1);
        RouteGraphEdge faster = edge(2, "ZZ", "AAA", "BBB", "08:00", "08:00", 100, 2);
        RouteGraph graph = graph(List.of(slower, faster), aircraft(1, 600), aircraft(2, 601));

        EarliestArrivalPath path = find(graph, "AAA", "BBB", "08:00");

        assertThat(path.legs().getFirst().edge()).isEqualTo(faster);
        assertThat(path.legs().getFirst().flightDuration()).isLessThan(java.time.Duration.ofMinutes(10));
    }

    @Test
    void cyclesDoNotBreakTraversal() {
        RouteGraph graph = graph(
                List.of(
                        edge(1, "CO", "AAA", "BBB", "08:00", "08:00", 10, 1),
                        edge(2, "CO", "BBB", "AAA", "08:10", "08:10", 10, 1),
                        edge(3, "CO", "BBB", "CCC", "08:20", "08:20", 10, 1)),
                aircraft(1, 600));

        assertThat(find(graph, "AAA", "CCC", "08:00").legs())
                .extracting(leg -> leg.edge().companyRouteId())
                .containsExactly(1L, 3L);
    }

    @Test
    void unreachableDestinationReturnsEmpty() {
        RouteGraph graph = graph(
                Set.of("AAA", "BBB", "CCC"),
                List.of(edge(1, "CO", "AAA", "BBB", "08:00", "08:00", 10, 1)),
                aircraft(1, 600));

        assertThat(finder.findRoute(graph, "AAA", "CCC", instant("08:00"))).isEmpty();
    }

    @Test
    void sameOriginReturnsZeroLegPathAtJourneyStart() {
        RouteGraph graph = graph(Set.of("AAA"), List.of(), aircraft(1, 600));

        EarliestArrivalPath path = find(graph, "AAA", "AAA", "08:00");

        assertThat(path.legs()).isEmpty();
        assertThat(path.journeyArrivalUtc()).isEqualTo(path.journeyStartUtc());
    }

    @Test
    void shuffledInputProducesSameDeterministicEqualArrivalPath() {
        List<RouteGraphEdge> edges = List.of(
                edge(4, "ZZ", "CCC", "DDD", "09:00", "09:00", 100, 1),
                edge(3, "ZZ", "AAA", "CCC", "08:00", "08:00", 100, 1),
                edge(2, "AA", "BBB", "DDD", "09:00", "09:00", 100, 1),
                edge(1, "AA", "AAA", "BBB", "08:00", "08:00", 100, 1));
        List<RouteGraphEdge> shuffled = new ArrayList<>(edges);
        Collections.shuffle(shuffled, new java.util.Random(42));

        List<Long> first = routeIds(find(graph(edges, aircraft(1, 100)), "AAA", "DDD", "08:00"));
        List<Long> second = routeIds(find(graph(shuffled, aircraft(1, 100)), "AAA", "DDD", "08:00"));

        assertThat(first).containsExactly(1L, 2L);
        assertThat(second).isEqualTo(first);
    }

    private List<Long> routeIds(EarliestArrivalPath path) {
        return path.legs().stream().map(leg -> leg.edge().companyRouteId()).toList();
    }

    private EarliestArrivalPath find(RouteGraph graph, String origin, String destination, String startTime) {
        return finder.findRoute(graph, origin, destination, instant(startTime)).orElseThrow();
    }

    private Instant instant(String time) {
        return Instant.parse("2026-10-20T" + time + ":00Z");
    }

    private RouteGraph graph(List<RouteGraphEdge> edges, AircraftTypeInfo... aircraft) {
        Set<String> airports = new LinkedHashSet<>();
        edges.forEach(edge -> {
            airports.add(edge.originAirportCode());
            airports.add(edge.destinationAirportCode());
        });
        return graph(airports, edges, aircraft);
    }

    private RouteGraph graph(
            Set<String> airportCodes,
            List<RouteGraphEdge> edges,
            AircraftTypeInfo... aircraft) {
        Map<String, AirportNode> airports = new LinkedHashMap<>();
        airportCodes.stream().sorted().forEach(code -> airports.put(code, new AirportNode(code, 0, 0, 0)));
        Map<Long, AircraftTypeInfo> aircraftTypes = new LinkedHashMap<>();
        for (AircraftTypeInfo type : aircraft) {
            aircraftTypes.put(type.id(), type);
        }
        Map<String, List<RouteGraphEdge>> outgoing = new LinkedHashMap<>();
        airports.keySet().forEach(code -> outgoing.put(code, new ArrayList<>()));
        edges.forEach(edge -> outgoing.get(edge.originAirportCode()).add(edge));
        return new RouteGraph(airports, aircraftTypes, Set.of("AA", "CO", "ZZ"), edges, outgoing);
    }

    private AircraftTypeInfo aircraft(long id, double speed) {
        return new AircraftTypeInfo(id, "Maker", "Model" + id, speed, 10000, 10000, 1, 100, 1);
    }

    private RouteGraphEdge edge(
            long id,
            String company,
            String origin,
            String destination,
            String scheduled,
            String adjusted,
            double distance,
            long aircraftTypeId) {
        return edge(id, company, origin, destination, scheduled, adjusted, distance, aircraftTypeId, 0);
    }

    private RouteGraphEdge edge(
            long id,
            String company,
            String origin,
            String destination,
            String scheduled,
            String adjusted,
            double distance,
            long aircraftTypeId,
            long delayMinutes) {
        return new RouteGraphEdge(
                id,
                (int) id,
                FlightDirection.OUTBOUND,
                company,
                aircraftTypeId,
                origin,
                destination,
                LocalTime.parse(scheduled),
                LocalTime.parse(adjusted),
                0,
                delayMinutes,
                distance);
    }
}
