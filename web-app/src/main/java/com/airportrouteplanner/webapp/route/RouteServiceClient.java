package com.airportrouteplanner.webapp.route;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RouteServiceClient implements RouteDataProvider {

    private final String baseUrl;
    private final RestClient suppliedRestClient;

    @Autowired
    public RouteServiceClient(
            @Value("${route-service.base-url:http://localhost:8084}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.suppliedRestClient = null;
    }

    RouteServiceClient(RestClient restClient) {
        this.baseUrl = null;
        this.suppliedRestClient = restClient;
    }

    @Override
    public RouteCalculationResponse calculate(RouteCalculationRequest request) {
        try {
            RestClient restClient = suppliedRestClient != null
                    ? suppliedRestClient
                    : RestClient.create(baseUrl);
            RouteCalculationResponse response = restClient.post()
                    .uri("/api/routes/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RouteCalculationResponse.class);
            if (response == null) {
                throw unavailable("route-service returned no route calculation");
            }
            return response;
        } catch (RouteCalculationException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw mapResponseFailure(exception);
        } catch (RestClientException exception) {
            throw unavailable("route-service is temporarily unavailable", exception);
        } catch (RuntimeException exception) {
            throw unavailable("route-service returned an unreadable response", exception);
        }
    }

    private static RouteCalculationException mapResponseFailure(RestClientResponseException exception) {
        HttpStatus upstreamStatus = HttpStatus.resolve(exception.getStatusCode().value());
        ProblemDetail upstreamProblem = readProblem(exception);
        String detail = upstreamProblem != null && upstreamProblem.getDetail() != null
                ? upstreamProblem.getDetail()
                : "route-service could not complete the calculation";

        if (upstreamStatus == HttpStatus.BAD_REQUEST) {
            return new RouteCalculationException(
                    HttpStatus.BAD_REQUEST, "Invalid route request", detail, exception);
        }
        if (upstreamStatus == HttpStatus.NOT_FOUND) {
            String title = upstreamProblem != null && upstreamProblem.getTitle() != null
                    ? upstreamProblem.getTitle()
                    : "Route not found";
            return new RouteCalculationException(HttpStatus.NOT_FOUND, title, detail, exception);
        }
        return unavailable(detail, exception);
    }

    private static ProblemDetail readProblem(RestClientResponseException exception) {
        try {
            return exception.getResponseBodyAs(ProblemDetail.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static RouteCalculationException unavailable(String detail) {
        return new RouteCalculationException(
                HttpStatus.BAD_GATEWAY, "Route calculation unavailable", detail);
    }

    private static RouteCalculationException unavailable(String detail, Throwable cause) {
        return new RouteCalculationException(
                HttpStatus.BAD_GATEWAY, "Route calculation unavailable", detail, cause);
    }
}
