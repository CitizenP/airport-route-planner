package com.airportrouteplanner.flightservice.airport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AirportServiceRunwayCapacityClient implements AirportRunwayCapacityProvider {

    private final String airportServiceBaseUrl;

    public AirportServiceRunwayCapacityClient(
            @Value("${airport-service.base-url:http://localhost:8081}") String airportServiceBaseUrl) {
        this.airportServiceBaseUrl = airportServiceBaseUrl;
    }

    @Override
    public Map<String, Integer> getRunwayCapacities() {
        try {
            List<AirportRunwayResponse> airports = RestClient.create(airportServiceBaseUrl).get()
                    .uri("/api/airports")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (airports == null) {
                throw new AirportRunwayCapacityException("airport-service returned no runway-capacity data");
            }

            Map<String, Integer> capacities = new LinkedHashMap<>();
            for (AirportRunwayResponse airport : airports) {
                Integer previous = capacities.putIfAbsent(airport.iataCode(), airport.numberOfRunways());
                if (previous != null) {
                    throw new AirportRunwayCapacityException(
                            "airport-service returned duplicate airport code " + airport.iataCode());
                }
            }
            return Map.copyOf(capacities);
        } catch (AirportRunwayCapacityException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AirportRunwayCapacityException(
                    "could not retrieve runway capacities from airport-service", exception);
        }
    }
}
