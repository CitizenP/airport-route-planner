package com.airportrouteplanner.routeservice.routing;

import com.airportrouteplanner.routeservice.graph.AirportNode;
import com.airportrouteplanner.routeservice.graph.RouteGraph;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FastestRouteCalculator {

    private final EarliestArrivalRouteFinder routeFinder;
    private final FixedOffsetUtcConverter utcConverter;
    private final StaticRouteCalculator metricCalculator;

    public FastestRouteCalculator(
            EarliestArrivalRouteFinder routeFinder,
            FixedOffsetUtcConverter utcConverter,
            StaticRouteCalculator metricCalculator) {
        this.routeFinder = routeFinder;
        this.utcConverter = utcConverter;
        this.metricCalculator = metricCalculator;
    }

    public FastestRouteResult calculate(
            RouteGraph graph,
            String originAirportCode,
            String destinationAirportCode,
            LocalDateTime departureLocalDateTime) {
        AirportNode origin = graph.airports().get(originAirportCode);
        if (origin == null) {
            throw new AirportNotFoundException(originAirportCode);
        }
        if (!graph.airports().containsKey(destinationAirportCode)) {
            throw new AirportNotFoundException(destinationAirportCode);
        }

        Instant journeyStart = utcConverter.toUtcInstant(
                departureLocalDateTime, origin.utcOffsetMinutes());
        EarliestArrivalPath path = routeFinder.findRoute(
                        graph, originAirportCode, destinationAirportCode, journeyStart)
                .orElseThrow(() -> new RouteNotFoundException(originAirportCode, destinationAirportCode));
        List<TimedRouteLeg> legs = path.legs();

        return new FastestRouteResult(
                originAirportCode,
                destinationAirportCode,
                path.journeyStartUtc(),
                path.journeyArrivalUtc(),
                legs,
                legs.stream().mapToDouble(leg -> leg.edge().distanceKm()).sum(),
                legs.stream().mapToDouble(leg -> metricCalculator.fuelLitres(graph, leg.edge())).sum(),
                legs.stream().mapToDouble(
                                leg -> metricCalculator.fuelPerPassengerLitres(graph, leg.edge()))
                        .sum());
    }
}
