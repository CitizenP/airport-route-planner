package com.airportrouteplanner.routeservice.integration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class FlightServiceAdjustedFlightLegClient implements AdjustedFlightLegDataProvider {

    private final UpstreamRestCollectionClient restClient;
    private final String baseUrl;

    public FlightServiceAdjustedFlightLegClient(
            UpstreamRestCollectionClient restClient,
            @Value("${flight-service.base-url:http://localhost:8083}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<AdjustedFlightLegClientResponse> getAdjustedFlightLegs() {
        return restClient.getCollection(
                "flight-service",
                baseUrl,
                "/api/flight-legs",
                new ParameterizedTypeReference<>() {});
    }
}
