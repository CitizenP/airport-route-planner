package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Component;

@Component
public class DijkstraRouteFinder {

    private static final Comparator<QueueEntry> QUEUE_ORDER = (left, right) -> {
        int costComparison = Double.compare(left.cost(), right.cost());
        if (costComparison != 0) {
            return costComparison;
        }
        int pathComparison = comparePaths(left.edges(), right.edges());
        if (pathComparison != 0) {
            return pathComparison;
        }
        return left.airportCode().compareTo(right.airportCode());
    };

    public Optional<DijkstraPath> findRoute(
            RouteGraph graph,
            String originAirportCode,
            String destinationAirportCode,
            ToDoubleFunction<RouteGraphEdge> edgeWeightFunction) {
        Map<RouteGraphEdge, Double> edgeWeights = validateAndCalculateWeights(graph, edgeWeightFunction);
        if (originAirportCode.equals(destinationAirportCode)) {
            return Optional.of(new DijkstraPath(List.of(), 0.0));
        }

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(QUEUE_ORDER);
        Map<String, BestPath> bestPaths = new HashMap<>();
        Set<String> settledAirports = new HashSet<>();
        BestPath origin = new BestPath(0.0, List.of());
        bestPaths.put(originAirportCode, origin);
        queue.add(new QueueEntry(originAirportCode, origin.cost(), origin.edges()));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.remove();
            BestPath currentBest = bestPaths.get(current.airportCode());
            if (currentBest == null
                    || Double.compare(current.cost(), currentBest.cost()) != 0
                    || comparePaths(current.edges(), currentBest.edges()) != 0
                    || !settledAirports.add(current.airportCode())) {
                continue;
            }
            if (current.airportCode().equals(destinationAirportCode)) {
                return Optional.of(new DijkstraPath(current.edges(), current.cost()));
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
                double candidateCost = current.cost() + edgeWeights.get(edge);
                if (!Double.isFinite(candidateCost)) {
                    throw new InvalidEdgeWeightException("route cost became non-finite");
                }
                List<RouteGraphEdge> candidateEdges = append(current.edges(), edge);
                BestPath existing = bestPaths.get(edge.destinationAirportCode());
                if (isBetter(candidateCost, candidateEdges, existing)) {
                    BestPath candidate = new BestPath(candidateCost, candidateEdges);
                    bestPaths.put(edge.destinationAirportCode(), candidate);
                    queue.add(new QueueEntry(
                            edge.destinationAirportCode(), candidate.cost(), candidate.edges()));
                }
            }
        }

        return Optional.empty();
    }

    private static Map<RouteGraphEdge, Double> validateAndCalculateWeights(
            RouteGraph graph,
            ToDoubleFunction<RouteGraphEdge> edgeWeightFunction) {
        Map<RouteGraphEdge, Double> weights = new IdentityHashMap<>();
        for (RouteGraphEdge edge : graph.edges()) {
            double weight = edgeWeightFunction.applyAsDouble(edge);
            if (!Double.isFinite(weight) || weight < 0) {
                throw new InvalidEdgeWeightException(
                        "edge weight must be finite and non-negative for route "
                                + edge.companyRouteId() + "/" + edge.direction());
            }
            weights.put(edge, weight);
        }
        return weights;
    }

    private static boolean isBetter(double candidateCost, List<RouteGraphEdge> candidateEdges, BestPath existing) {
        if (existing == null) {
            return true;
        }
        int costComparison = Double.compare(candidateCost, existing.cost());
        return costComparison < 0
                || (costComparison == 0 && comparePaths(candidateEdges, existing.edges()) < 0);
    }

    private static List<RouteGraphEdge> append(List<RouteGraphEdge> edges, RouteGraphEdge edge) {
        List<RouteGraphEdge> result = new ArrayList<>(edges.size() + 1);
        result.addAll(edges);
        result.add(edge);
        return List.copyOf(result);
    }

    private static int comparePaths(List<RouteGraphEdge> left, List<RouteGraphEdge> right) {
        int commonSize = Math.min(left.size(), right.size());
        for (int index = 0; index < commonSize; index++) {
            int comparison = RouteEdgeOrdering.DETERMINISTIC.compare(left.get(index), right.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private record BestPath(double cost, List<RouteGraphEdge> edges) {
    }

    private record QueueEntry(String airportCode, double cost, List<RouteGraphEdge> edges) {
    }
}
