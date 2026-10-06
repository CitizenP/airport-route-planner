package com.airportrouteplanner.routeservice.integration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class FleetServiceFlightCompanyClient implements FlightCompanyDataProvider {

    private final UpstreamRestCollectionClient restClient;
    private final String baseUrl;

    public FleetServiceFlightCompanyClient(
            UpstreamRestCollectionClient restClient,
            @Value("${fleet-service.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<FlightCompanyClientResponse> getFlightCompanies() {
        return restClient.getCollection(
                "fleet-service",
                baseUrl,
                "/api/flight-companies",
                new ParameterizedTypeReference<>() {});
    }
}
