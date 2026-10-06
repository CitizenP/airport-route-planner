package com.airportrouteplanner.routeservice.graph;

import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegClientResponse;
import com.airportrouteplanner.routeservice.integration.AircraftTypeClientResponse;
import com.airportrouteplanner.routeservice.integration.AirportClientResponse;
import com.airportrouteplanner.routeservice.integration.FlightCompanyClientResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RouteGraphBuilder {

    private final HaversineDistanceCalculator distanceCalculator;

    public RouteGraphBuilder(HaversineDistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public RouteGraph build(
            List<AirportClientResponse> airportResponses,
            List<AircraftTypeClientResponse> aircraftTypeResponses,
            List<FlightCompanyClientResponse> companyResponses,
            List<AdjustedFlightLegClientResponse> flightLegResponses) {
        Map<String, AirportNode> airports = indexAirports(airportResponses);
        Map<Long, AircraftTypeInfo> aircraftTypes = indexAircraftTypes(aircraftTypeResponses);
        Set<String> companyCodes = indexCompanyCodes(companyResponses);
        List<RouteGraphEdge> edges = buildEdges(
                flightLegResponses, airports, aircraftTypes, companyCodes);

        Map<String, List<RouteGraphEdge>> outgoingEdges = new LinkedHashMap<>();
        airports.keySet().forEach(code -> outgoingEdges.put(code, new ArrayList<>()));
        edges.forEach(edge -> outgoingEdges.get(edge.originAirportCode()).add(edge));

        return new RouteGraph(airports, aircraftTypes, companyCodes, edges, outgoingEdges);
    }

    private Map<String, AirportNode> indexAirports(List<AirportClientResponse> responses) {
        Map<String, AirportNode> airports = new LinkedHashMap<>();
        for (AirportClientResponse response : requireList(responses, "airport data")) {
            if (response == null || response.iataCode() == null || !response.iataCode().matches("[A-Z]{3}")) {
                throw new RouteGraphValidationException("airport data contains an invalid IATA code");
            }
            AirportNode node = new AirportNode(
                    response.iataCode(), response.latitude(), response.longitude(), response.utcOffsetMinutes());
            validateAirportCoordinates(node);
            if (airports.putIfAbsent(node.iataCode(), node) != null) {
                throw new RouteGraphValidationException("duplicate airport IATA code " + node.iataCode());
            }
        }
        return airports;
    }

    private Map<Long, AircraftTypeInfo> indexAircraftTypes(List<AircraftTypeClientResponse> responses) {
        Map<Long, AircraftTypeInfo> aircraftTypes = new LinkedHashMap<>();
        for (AircraftTypeClientResponse response : requireList(responses, "aircraft-type data")) {
            if (response == null || response.id() == null || response.id() <= 0) {
                throw new RouteGraphValidationException("aircraft-type data contains an invalid ID");
            }
            if (!Double.isFinite(response.maximumRangeKm()) || response.maximumRangeKm() <= 0) {
                throw new RouteGraphValidationException(
                        "aircraft type " + response.id() + " has invalid maximum range");
            }
            AircraftTypeInfo aircraftType = new AircraftTypeInfo(
                    response.id(),
                    response.manufacturer(),
                    response.model(),
                    response.cruiseSpeedKmH(),
                    response.maximumRangeKm(),
                    response.fuelCapacityLitres(),
                    response.fuelConsumptionLitresPerKm(),
                    response.passengerCapacity(),
                    response.fuelConsumptionPerPassenger());
            if (aircraftTypes.putIfAbsent(aircraftType.id(), aircraftType) != null) {
                throw new RouteGraphValidationException("duplicate aircraft type ID " + aircraftType.id());
            }
        }
        return aircraftTypes;
    }

    private Set<String> indexCompanyCodes(List<FlightCompanyClientResponse> responses) {
        Set<String> companyCodes = new LinkedHashSet<>();
        for (FlightCompanyClientResponse response : requireList(responses, "flight-company data")) {
            if (response == null || response.code() == null || response.code().isBlank()) {
                throw new RouteGraphValidationException("flight-company data contains an invalid code");
            }
            if (!companyCodes.add(response.code())) {
                throw new RouteGraphValidationException("duplicate flight-company code " + response.code());
            }
        }
        return companyCodes;
    }

    private List<RouteGraphEdge> buildEdges(
            List<AdjustedFlightLegClientResponse> responses,
            Map<String, AirportNode> airports,
            Map<Long, AircraftTypeInfo> aircraftTypes,
            Set<String> companyCodes) {
        List<RouteGraphEdge> edges = new ArrayList<>();
        Set<FlightLegIdentity> identities = new LinkedHashSet<>();

        for (AdjustedFlightLegClientResponse response : requireList(responses, "adjusted flight-leg data")) {
            validateRequiredEdgeFields(response);
            FlightLegIdentity identity = new FlightLegIdentity(response.companyRouteId(), response.direction());
            if (!identities.add(identity)) {
                throw new RouteGraphValidationException(
                        "duplicate flight-leg identity " + response.companyRouteId() + "/" + response.direction());
            }

            AirportNode origin = airports.get(response.originAirportCode());
            if (origin == null) {
                throw new RouteGraphValidationException(
                        "missing origin airport " + response.originAirportCode());
            }
            AirportNode destination = airports.get(response.destinationAirportCode());
            if (destination == null) {
                throw new RouteGraphValidationException(
                        "missing destination airport " + response.destinationAirportCode());
            }
            if (origin.iataCode().equals(destination.iataCode())) {
                throw new RouteGraphValidationException(
                        "flight leg " + response.companyRouteId() + "/" + response.direction()
                                + " has identical origin and destination");
            }

            AircraftTypeInfo aircraftType = aircraftTypes.get(response.aircraftTypeId());
            if (aircraftType == null) {
                throw new RouteGraphValidationException(
                        "missing aircraft type " + response.aircraftTypeId());
            }
            if (!companyCodes.contains(response.companyCode())) {
                throw new RouteGraphValidationException("missing flight company " + response.companyCode());
            }
            if (response.delayMinutes() < 0) {
                throw new RouteGraphValidationException("flight-leg delayMinutes must not be negative");
            }
            if (response.adjustedDepartureDayOffset() < 0) {
                throw new RouteGraphValidationException(
                        "flight-leg adjustedDepartureDayOffset must not be negative");
            }

            double distanceKm = calculateDistance(origin, destination);
            if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
                throw new RouteGraphValidationException(
                        "flight leg " + response.companyRouteId() + "/" + response.direction()
                                + " has invalid distance");
            }
            if (distanceKm > aircraftType.maximumRangeKm()) {
                throw new RouteGraphValidationException(
                        "flight leg " + response.companyRouteId() + "/" + response.direction()
                                + " distance exceeds aircraft type " + aircraftType.id() + " range");
            }

            edges.add(new RouteGraphEdge(
                    response.companyRouteId(),
                    response.routeNumber(),
                    response.direction(),
                    response.companyCode(),
                    response.aircraftTypeId(),
                    response.originAirportCode(),
                    response.destinationAirportCode(),
                    response.scheduledDepartureUtc(),
                    response.adjustedDepartureUtc(),
                    response.adjustedDepartureDayOffset(),
                    response.delayMinutes(),
                    distanceKm));
        }
        return List.copyOf(edges);
    }

    private double calculateDistance(AirportNode origin, AirportNode destination) {
        try {
            return distanceCalculator.calculateKilometres(
                    origin.latitude(), origin.longitude(), destination.latitude(), destination.longitude());
        } catch (IllegalArgumentException exception) {
            throw new RouteGraphValidationException(
                    "invalid coordinates for edge " + origin.iataCode() + " to " + destination.iataCode(),
                    exception);
        }
    }

    private void validateAirportCoordinates(AirportNode airport) {
        try {
            distanceCalculator.calculateKilometres(
                    airport.latitude(), airport.longitude(), airport.latitude(), airport.longitude());
        } catch (IllegalArgumentException exception) {
            throw new RouteGraphValidationException(
                    "invalid coordinates for airport " + airport.iataCode(), exception);
        }
    }

    private static void validateRequiredEdgeFields(AdjustedFlightLegClientResponse response) {
        if (response == null
                || response.companyRouteId() == null
                || response.direction() == null
                || response.companyCode() == null
                || response.aircraftTypeId() == null
                || response.originAirportCode() == null
                || response.destinationAirportCode() == null
                || response.scheduledDepartureUtc() == null
                || response.adjustedDepartureUtc() == null) {
            throw new RouteGraphValidationException("adjusted flight-leg data contains missing required values");
        }
    }

    private static <T> List<T> requireList(List<T> values, String name) {
        if (values == null) {
            throw new RouteGraphValidationException(name + " is unavailable");
        }
        return values;
    }

    private record FlightLegIdentity(Long companyRouteId, FlightDirection direction) {
    }
}
