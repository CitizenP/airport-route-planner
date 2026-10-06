package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.AircraftTypeInfo;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EarliestArrivalRouteFinder {

    private static final Comparator<QueueEntry> QUEUE_ORDER = (left, right) -> {
        int arrivalComparison = left.arrival().compareTo(right.arrival());
        if (arrivalComparison != 0) {
            return arrivalComparison;
        }
        int pathComparison = RouteEdgeOrdering.comparePaths(edges(left.legs()), edges(right.legs()));
        if (pathComparison != 0) {
            return pathComparison;
        }
        return left.airportCode().compareTo(right.airportCode());
    };

    private final DailyFlightSchedule dailyFlightSchedule;
    private final FlightDurationCalculator flightDurationCalculator;

    public EarliestArrivalRouteFinder(
            DailyFlightSchedule dailyFlightSchedule,
            FlightDurationCalculator flightDurationCalculator) {
        this.dailyFlightSchedule = dailyFlightSchedule;
        this.flightDurationCalculator = flightDurationCalculator;
    }

    public Optional<EarliestArrivalPath> findRoute(
            RouteGraph graph,
            String originAirportCode,
            String destinationAirportCode,
            Instant journeyStartUtc) {
        if (originAirportCode.equals(destinationAirportCode)) {
            return Optional.of(new EarliestArrivalPath(journeyStartUtc, journeyStartUtc, List.of()));
        }

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(QUEUE_ORDER);
        Map<String, BestArrival> bestArrivals = new HashMap<>();
        Set<String> settledAirports = new HashSet<>();
        BestArrival origin = new BestArrival(journeyStartUtc, List.of());
        bestArrivals.put(originAirportCode, origin);
        queue.add(new QueueEntry(originAirportCode, origin.arrival(), origin.legs()));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.remove();
            BestArrival currentBest = bestArrivals.get(current.airportCode());
            if (currentBest == null
                    || !current.arrival().equals(currentBest.arrival())
                    || compareLegPaths(current.legs(), currentBest.legs()) != 0
                    || !settledAirports.add(current.airportCode())) {
                continue;
            }
            if (current.airportCode().equals(destinationAirportCode)) {
                return Optional.of(new EarliestArrivalPath(
                        journeyStartUtc, current.arrival(), current.legs()));
            }

            List<RouteGraphEdge> outgoingEdges = graph.outgoingEdges()
                    .getOrDefault(current.airportCode(), List.of())
                    .stream()
                    .sorted(RouteEdgeOrdering.DETERMINISTIC)
                    .toList();
            for (RouteGraphEdge edge : outgoingEdges) {
                if (settledAirports.contains(edge.destinationAirportCode())) {
                    continue;
                }
                DailyFlightSchedule.DepartureOccurrence departure =
                        dailyFlightSchedule.nextDeparture(edge, current.arrival());
                Duration waiting = Duration.between(current.arrival(), departure.adjustedDepartureInstant());
                AircraftTypeInfo aircraft = graph.aircraftTypes().get(edge.aircraftTypeId());
                if (aircraft == null) {
                    throw new InvalidEdgeWeightException("missing aircraft type " + edge.aircraftTypeId());
                }
                Duration flightDuration = flightDurationCalculator.calculate(
                        edge.distanceKm(), aircraft.cruiseSpeedKmH());
                Instant arrival = departure.adjustedDepartureInstant().plus(flightDuration);
                TimedRouteLeg leg = new TimedRouteLeg(
                        edge,
                        departure.scheduledDepartureInstant(),
                        departure.adjustedDepartureInstant(),
                        arrival,
                        waiting,
                        flightDuration);
                List<TimedRouteLeg> candidateLegs = append(current.legs(), leg);
                BestArrival existing = bestArrivals.get(edge.destinationAirportCode());
                if (isBetter(arrival, candidateLegs, existing)) {
                    BestArrival candidate = new BestArrival(arrival, candidateLegs);
                    bestArrivals.put(edge.destinationAirportCode(), candidate);
                    queue.add(new QueueEntry(
                            edge.destinationAirportCode(), candidate.arrival(), candidate.legs()));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isBetter(
            Instant candidateArrival,
            List<TimedRouteLeg> candidateLegs,
            BestArrival existing) {
        if (existing == null) {
            return true;
        }
        int arrivalComparison = candidateArrival.compareTo(existing.arrival());
        return arrivalComparison < 0
                || (arrivalComparison == 0 && compareLegPaths(candidateLegs, existing.legs()) < 0);
    }

    private static List<TimedRouteLeg> append(List<TimedRouteLeg> legs, TimedRouteLeg leg) {
        List<TimedRouteLeg> result = new ArrayList<>(legs.size() + 1);
        result.addAll(legs);
        result.add(leg);
        return List.copyOf(result);
    }

    private static int compareLegPaths(List<TimedRouteLeg> left, List<TimedRouteLeg> right) {
        return RouteEdgeOrdering.comparePaths(edges(left), edges(right));
    }

    private static List<RouteGraphEdge> edges(List<TimedRouteLeg> legs) {
        return legs.stream().map(TimedRouteLeg::edge).toList();
    }

    private record BestArrival(Instant arrival, List<TimedRouteLeg> legs) {
    }

    private record QueueEntry(String airportCode, Instant arrival, List<TimedRouteLeg> legs) {
    }
}
