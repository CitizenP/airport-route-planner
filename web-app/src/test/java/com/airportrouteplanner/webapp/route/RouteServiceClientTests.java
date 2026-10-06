package com.airportrouteplanner.webapp.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RouteServiceClientTests {

    private MockRestServiceServer server;
    private RouteServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RouteServiceClient(builder.baseUrl("http://route-service.test").build());
    }

    @Test
    void postsSemanticRequestAndReadsRouteResponse() {
        server.expect(requestTo("http://route-service.test/api/routes/calculate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "originAirportCode": "LIS",
                          "destinationAirportCode": "ATL",
                          "routeType": "SHORTEST",
                          "departureLocalDateTime": null
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "routeType": "SHORTEST",
                          "originAirportCode": "LIS",
                          "destinationAirportCode": "ATL",
                          "totalDistanceKm": 8706.599,
                          "totalFuelLitres": 59450.901,
                          "fuelPerPassengerLitres": 223.210,
                          "totalJourneyMinutes": null,
                          "journeyStartUtc": null,
                          "journeyArrivalUtc": null,
                          "legs": []
                        }
                        """, MediaType.APPLICATION_JSON));

        RouteCalculationResponse response = client.calculate(
                new RouteCalculationRequest("LIS", "ATL", RouteType.SHORTEST, null));

        assertThat(response.totalDistanceKm()).isEqualTo(8706.599);
        assertThat(response.totalFuelLitres()).isEqualTo(59450.901);
        assertThat(response.fuelPerPassengerLitres()).isEqualTo(223.210);
        server.verify();
    }

    @Test
    void preservesBadRequestMeaning() {
        expectProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid route request",
                "departureLocalDateTime is required for FASTEST routing");

        assertThatThrownBy(() -> client.calculate(
                new RouteCalculationRequest("RIX", "ARN", RouteType.FASTEST, null)))
                .isInstanceOfSatisfying(RouteCalculationException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.title()).isEqualTo("Invalid route request");
                    assertThat(exception.getMessage())
                            .isEqualTo("departureLocalDateTime is required for FASTEST routing");
                });
    }

    @Test
    void preservesNotFoundMeaning() {
        expectProblem(HttpStatus.NOT_FOUND, "Airport not found", "unknown airport code: XXX");

        assertThatThrownBy(() -> client.calculate(
                new RouteCalculationRequest("XXX", "ATL", RouteType.SHORTEST, null)))
                .isInstanceOfSatisfying(RouteCalculationException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.title()).isEqualTo("Airport not found");
                    assertThat(exception.getMessage()).isEqualTo("unknown airport code: XXX");
                });
    }

    @Test
    void transportFailureBecomesBadGatewayWithoutLeakingExceptionDetails() {
        server.expect(requestTo("http://route-service.test/api/routes/calculate"))
                .andRespond(withException(new IOException("low-level connection detail")));

        assertThatThrownBy(() -> client.calculate(
                new RouteCalculationRequest("LIS", "ATL", RouteType.SHORTEST, null)))
                .isInstanceOfSatisfying(RouteCalculationException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.title()).isEqualTo("Route calculation unavailable");
                    assertThat(exception.getMessage()).isEqualTo("route-service is temporarily unavailable");
                    assertThat(exception.getMessage()).doesNotContain("low-level connection detail");
                });
    }

    private void expectProblem(HttpStatus status, String title, String detail) {
        server.expect(requestTo("http://route-service.test/api/routes/calculate"))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "about:blank",
                                  "title": "%s",
                                  "status": %d,
                                  "detail": "%s"
                                }
                                """.formatted(title, status.value(), detail)));
    }
}
