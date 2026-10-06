package com.airportrouteplanner.routeservice.integration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class FleetServiceAircraftTypeClient implements AircraftTypeDataProvider {

    private final UpstreamRestCollectionClient restClient;
    private final String baseUrl;

    public FleetServiceAircraftTypeClient(
            UpstreamRestCollectionClient restClient,
            @Value("${fleet-service.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<AircraftTypeClientResponse> getAircraftTypes() {
        return restClient.getCollection(
                "fleet-service",
                baseUrl,
                "/api/aircraft-types",
                new ParameterizedTypeReference<>() {});
    }
}
