package com.airportrouteplanner.webapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airportrouteplanner.webapp.route.FlightDirection;
import com.airportrouteplanner.webapp.route.RouteCalculationException;
import com.airportrouteplanner.webapp.route.RouteCalculationRequest;
import com.airportrouteplanner.webapp.route.RouteCalculationResponse;
import com.airportrouteplanner.webapp.route.RouteDataProvider;
import com.airportrouteplanner.webapp.route.RouteLegResponse;
import com.airportrouteplanner.webapp.route.RouteType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(RouteUiControllerIntegrationTests.RouteTestConfiguration.class)
class RouteUiControllerIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StubRouteDataProvider routeDataProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        routeDataProvider.response = shortestResponse(RouteType.SHORTEST);
        routeDataProvider.failure = null;
        routeDataProvider.lastRequest = null;
    }

    @Test
    void shortestCalculationForwardsRequestAndPreservesTotals() throws Exception {
        mockMvc.perform(post("/api/ui/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originAirportCode": "lis",
                                  "destinationAirportCode": "atl",
                                  "routeType": "SHORTEST"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("SHORTEST"))
                .andExpect(jsonPath("$.totalDistanceKm").value(8706.599))
                .andExpect(jsonPath("$.totalFuelLitres").value(59450.901))
                .andExpect(jsonPath("$.fuelPerPassengerLitres").value(223.210))
                .andExpect(jsonPath("$.legs[0].originAirportCode").value("LIS"));

        org.assertj.core.api.Assertions.assertThat(routeDataProvider.lastRequest)
                .isEqualTo(new RouteCalculationRequest("lis", "atl", RouteType.SHORTEST, null));
    }

    @Test
    void staticCalculationDoesNotRequireDepartureDateTime() throws Exception {
        routeDataProvider.response = shortestResponse(RouteType.CHEAPEST);

        mockMvc.perform(post("/api/ui/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originAirportCode": "LIS",
                                  "destinationAirportCode": "ATL",
                                  "routeType": "CHEAPEST",
                                  "departureLocalDateTime": "2026-10-20T16:01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("CHEAPEST"));

        org.assertj.core.api.Assertions.assertThat(routeDataProvider.lastRequest.departureLocalDateTime()).isNull();
    }

    @Test
    void fastestCalculationForwardsLocalDateTimeAndPreservesAbsoluteTiming() throws Exception {
        routeDataProvider.response = fastestResponse();

        mockMvc.perform(post("/api/ui/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originAirportCode": "RIX",
                                  "destinationAirportCode": "ARN",
                                  "routeType": "FASTEST",
                                  "departureLocalDateTime": "2026-10-20T16:01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyStartUtc").value("2026-10-20T14:01:00Z"))
                .andExpect(jsonPath("$.journeyArrivalUtc").value("2026-10-20T14:36:45.215Z"))
                .andExpect(jsonPath("$.totalJourneyMinutes").value(35.7535833333))
                .andExpect(jsonPath("$.legs[0].adjustedDepartureInstant").value("2026-10-20T14:05:00Z"))
                .andExpect(jsonPath("$.legs[0].arrivalInstant").value("2026-10-20T14:36:45.215Z"))
                .andExpect(jsonPath("$.legs[0].waitingMinutes").value(4.0))
                .andExpect(jsonPath("$.legs[0].flightDurationMinutes").value(31.7535833333));

        org.assertj.core.api.Assertions.assertThat(routeDataProvider.lastRequest.departureLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 10, 20, 16, 1));
    }

    @Test
    void routeServiceBadRequestMapsCleanly() throws Exception {
        routeDataProvider.failure = new RouteCalculationException(
                HttpStatus.BAD_REQUEST, "Invalid route request", "departureLocalDateTime is required");

        assertProblem(HttpStatus.BAD_REQUEST, "Invalid route request", "departureLocalDateTime is required");
    }

    @Test
    void routeServiceNotFoundMapsCleanly() throws Exception {
        routeDataProvider.failure = new RouteCalculationException(
                HttpStatus.NOT_FOUND, "Route not found", "no directed route exists from AAA to BBB");

        assertProblem(HttpStatus.NOT_FOUND, "Route not found", "no directed route exists from AAA to BBB");
    }

    @Test
    void routeServiceUnavailableMapsToBadGateway() throws Exception {
        routeDataProvider.failure = new RouteCalculationException(
                HttpStatus.BAD_GATEWAY, "Route calculation unavailable", "route-service is temporarily unavailable");

        assertProblem(
                HttpStatus.BAD_GATEWAY,
                "Route calculation unavailable",
                "route-service is temporarily unavailable");
    }

    private void assertProblem(HttpStatus status, String title, String detail) throws Exception {
        mockMvc.perform(post("/api/ui/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originAirportCode": "AAA",
                                  "destinationAirportCode": "BBB",
                                  "routeType": "SHORTEST"
                                }
                                """))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private static RouteCalculationResponse shortestResponse(RouteType routeType) {
        return new RouteCalculationResponse(
                routeType,
                "LIS",
                "ATL",
                8706.599,
                59450.901,
                223.210,
                null,
                null,
                null,
                List.of(new RouteLegResponse(
                        1L,
                        1,
                        FlightDirection.OUTBOUND,
                        "TP",
                        9L,
                        "LIS",
                        "ATL",
                        8706.599,
                        59450.901,
                        223.210,
                        LocalTime.of(8, 0),
                        LocalTime.of(8, 0),
                        0,
                        0,
                        null,
                        null,
                        null,
                        null,
                        null)));
    }

    private static RouteCalculationResponse fastestResponse() {
        Instant start = Instant.parse("2026-10-20T14:01:00Z");
        Instant departure = Instant.parse("2026-10-20T14:05:00Z");
        Instant arrival = Instant.parse("2026-10-20T14:36:45.215Z");
        return new RouteCalculationResponse(
                RouteType.FASTEST,
                "RIX",
                "ARN",
                465.719326,
                3725.754608,
                11.596411,
                35.7535833333,
                start,
                arrival,
                List.of(new RouteLegResponse(
                        412L,
                        4,
                        FlightDirection.RETURN,
                        "LE",
                        8L,
                        "RIX",
                        "ARN",
                        465.719326,
                        3725.754608,
                        11.596411,
                        LocalTime.of(14, 0),
                        LocalTime.of(14, 5),
                        0,
                        5,
                        Instant.parse("2026-10-20T14:00:00Z"),
                        departure,
                        arrival,
                        4.0,
                        31.7535833333)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RouteTestConfiguration {

        @Bean
        @Primary
        StubRouteDataProvider stubRouteDataProvider() {
            return new StubRouteDataProvider();
        }
    }

    static class StubRouteDataProvider implements RouteDataProvider {

        private RouteCalculationResponse response;
        private RouteCalculationException failure;
        private RouteCalculationRequest lastRequest;

        @Override
        public RouteCalculationResponse calculate(RouteCalculationRequest request) {
            lastRequest = request;
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
