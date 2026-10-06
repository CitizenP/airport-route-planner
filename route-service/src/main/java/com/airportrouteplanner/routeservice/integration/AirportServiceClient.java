package com.airportrouteplanner.routeservice.integration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class AirportServiceClient implements AirportDataProvider {

    private final UpstreamRestCollectionClient restClient;
    private final String baseUrl;

    public AirportServiceClient(
            UpstreamRestCollectionClient restClient,
            @Value("${airport-service.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<AirportClientResponse> getAirports() {
        return restClient.getCollection(
                "airport-service",
                baseUrl,
                "/api/airports",
                new ParameterizedTypeReference<>() {});
    }
}
