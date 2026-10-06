package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.AircraftTypeInfo;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import com.airportrouteplanner.routeservice.graph.RouteGraphEdge;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Component;

@Component
public class StaticRouteCalculator {

    private final DijkstraRouteFinder routeFinder;

    public StaticRouteCalculator(DijkstraRouteFinder routeFinder) {
        this.routeFinder = routeFinder;
    }

    public StaticRouteResult calculate(
            RouteGraph graph,
            String originAirportCode,
            String destinationAirportCode,
            RouteType routeType) {
        if (routeType == RouteType.FASTEST) {
            throw new FastestRouteNotImplementedException();
        }
        if (!graph.airports().containsKey(originAirportCode)) {
            throw new AirportNotFoundException(originAirportCode);
        }
        if (!graph.airports().containsKey(destinationAirportCode)) {
            throw new AirportNotFoundException(destinationAirportCode);
        }

        ToDoubleFunction<RouteGraphEdge> edgeWeight = switch (routeType) {
            case SHORTEST -> RouteGraphEdge::distanceKm;
            case CHEAPEST -> edge -> fuelLitres(graph, edge);
            case ECOLOGICAL -> edge -> fuelPerPassengerLitres(graph, edge);
            case FASTEST -> throw new FastestRouteNotImplementedException();
        };

        DijkstraPath path = routeFinder.findRoute(graph, originAirportCode, destinationAirportCode, edgeWeight)
                .orElseThrow(() -> new RouteNotFoundException(originAirportCode, destinationAirportCode));
        List<RouteGraphEdge> edges = path.edges();
        double totalDistanceKm = edges.stream().mapToDouble(RouteGraphEdge::distanceKm).sum();
        double totalFuelLitres = edges.stream().mapToDouble(edge -> fuelLitres(graph, edge)).sum();
        double totalFuelPerPassenger = edges.stream()
                .mapToDouble(edge -> fuelPerPassengerLitres(graph, edge))
                .sum();

        return new StaticRouteResult(
                routeType,
                originAirportCode,
                destinationAirportCode,
                edges,
                path.optimizedCost(),
                totalDistanceKm,
                totalFuelLitres,
                totalFuelPerPassenger);
    }

    public double fuelLitres(RouteGraph graph, RouteGraphEdge edge) {
        return edge.distanceKm() * aircraftType(graph, edge).fuelConsumptionLitresPerKm();
    }

    public double fuelPerPassengerLitres(RouteGraph graph, RouteGraphEdge edge) {
        return (edge.distanceKm() / 100.0) * aircraftType(graph, edge).fuelConsumptionPerPassenger();
    }

    private AircraftTypeInfo aircraftType(RouteGraph graph, RouteGraphEdge edge) {
        AircraftTypeInfo aircraftType = graph.aircraftTypes().get(edge.aircraftTypeId());
        if (aircraftType == null) {
            throw new InvalidEdgeWeightException("missing aircraft type " + edge.aircraftTypeId());
        }
        return aircraftType;
    }
}
