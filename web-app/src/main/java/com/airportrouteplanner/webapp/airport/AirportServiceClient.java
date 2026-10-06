package com.airportrouteplanner.webapp.airport;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AirportServiceClient implements AirportDataProvider {

    private final String baseUrl;

    public AirportServiceClient(
            @Value("${airport-service.base-url:http://localhost:8081}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<AirportClientResponse> getAirports() {
        try {
            List<AirportClientResponse> airports = RestClient.create(baseUrl).get()
                    .uri("/api/airports")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (airports == null) {
                throw new AirportServiceUnavailableException("airport-service returned no airport data");
            }
            return List.copyOf(airports);
        } catch (AirportServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AirportServiceUnavailableException(
                    "could not retrieve airports from airport-service", exception);
        } catch (RuntimeException exception) {
            throw new AirportServiceUnavailableException(
                    "could not read airport data from airport-service", exception);
        }
    }
}
