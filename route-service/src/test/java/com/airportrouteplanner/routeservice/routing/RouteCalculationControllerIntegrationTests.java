package com.airportrouteplanner.routeservice.routing;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airportrouteplanner.routeservice.graph.FlightDirection;
import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegClientResponse;
import com.airportrouteplanner.routeservice.integration.AdjustedFlightLegDataProvider;
import com.airportrouteplanner.routeservice.integration.AircraftTypeClientResponse;
import com.airportrouteplanner.routeservice.integration.AircraftTypeDataProvider;
import com.airportrouteplanner.routeservice.integration.AirportClientResponse;
import com.airportrouteplanner.routeservice.integration.AirportDataProvider;
import com.airportrouteplanner.routeservice.integration.FlightCompanyClientResponse;
import com.airportrouteplanner.routeservice.integration.FlightCompanyDataProvider;
import com.airportrouteplanner.routeservice.integration.UpstreamServiceException;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(RouteCalculationControllerIntegrationTests.UpstreamTestConfiguration.class)
class RouteCalculationControllerIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestUpstreamData upstreamData;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        upstreamData.reset();
    }

    @ParameterizedTest
    @EnumSource(value = RouteType.class, names = {"SHORTEST", "CHEAPEST", "ECOLOGICAL"})
    void calculatesEachImplementedStaticRouteType(RouteType routeType) throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "BBB", routeType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value(routeType.name()))
                .andExpect(jsonPath("$.originAirportCode").value("AAA"))
                .andExpect(jsonPath("$.destinationAirportCode").value("BBB"))
                .andExpect(jsonPath("$.totalDistanceKm").isNumber())
                .andExpect(jsonPath("$.totalFuelLitres").isNumber())
                .andExpect(jsonPath("$.fuelPerPassengerLitres").isNumber())
                .andExpect(jsonPath("$.totalJourneyMinutes").value(nullValue()))
                .andExpect(jsonPath("$.journeyStartUtc").value(nullValue()))
                .andExpect(jsonPath("$.journeyArrivalUtc").value(nullValue()))
                .andExpect(jsonPath("$.legs.length()").value(1))
                .andExpect(jsonPath("$.legs[0].scheduledDepartureUtc").value("09:00:00"))
                .andExpect(jsonPath("$.legs[0].adjustedDepartureUtc").value("09:05:00"))
                .andExpect(jsonPath("$.legs[0].delayMinutes").value(5))
                .andExpect(jsonPath("$.legs[0].arrivalInstant").value(nullValue()));
    }

    @Test
    void normalizesLowercaseAirportCodes() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("aaa", "bbb", RouteType.SHORTEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originAirportCode").value("AAA"))
                .andExpect(jsonPath("$.destinationAirportCode").value("BBB"));
    }

    @Test
    void sameOriginAndDestinationReturnsZeroLegRoute() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "AAA", RouteType.CHEAPEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceKm").value(0))
                .andExpect(jsonPath("$.totalFuelLitres").value(0))
                .andExpect(jsonPath("$.fuelPerPassengerLitres").value(0))
                .andExpect(jsonPath("$.legs.length()").value(0));
    }

    @Test
    void unknownOriginReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("ZZZ", "BBB", RouteType.SHORTEST)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Airport not found"))
                .andExpect(jsonPath("$.detail").value("unknown airport code ZZZ"));
    }

    @Test
    void unknownDestinationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "ZZZ", RouteType.SHORTEST)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("unknown airport code ZZZ"));
    }

    @Test
    void unreachableDestinationReturnsRouteNotFound() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "CCC", RouteType.SHORTEST)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Route not found"))
                .andExpect(jsonPath("$.detail").value("no directed route from AAA to CCC"));
    }

    @Test
    void fastestReturnsAbsoluteTimingAndUsesAdjustedDeparture() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("aaa", "bbb", RouteType.FASTEST, "2026-10-20T08:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("FASTEST"))
                .andExpect(jsonPath("$.originAirportCode").value("AAA"))
                .andExpect(jsonPath("$.journeyStartUtc").value("2026-10-20T08:00:00Z"))
                .andExpect(jsonPath("$.journeyArrivalUtc").isString())
                .andExpect(jsonPath("$.totalJourneyMinutes").isNumber())
                .andExpect(jsonPath("$.legs[0].scheduledDepartureInstant").value("2026-10-20T09:00:00Z"))
                .andExpect(jsonPath("$.legs[0].adjustedDepartureInstant").value("2026-10-20T09:05:00Z"))
                .andExpect(jsonPath("$.legs[0].waitingMinutes").value(65.0))
                .andExpect(jsonPath("$.legs[0].flightDurationMinutes").isNumber());
    }

    @Test
    void fastestRequiresDepartureLocalDateTime() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "BBB", RouteType.FASTEST)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid route request"))
                .andExpect(jsonPath("$.detail")
                        .value("departureLocalDateTime is required for FASTEST routing"));
    }

    @Test
    void fastestUnknownOriginReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("ZZZ", "BBB", RouteType.FASTEST, "2026-10-20T08:00:00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void fastestUnknownDestinationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "ZZZ", RouteType.FASTEST, "2026-10-20T08:00:00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void fastestUnreachableDestinationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "CCC", RouteType.FASTEST, "2026-10-20T08:00:00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Route not found"));
    }

    @Test
    void fastestSameOriginReturnsZeroLegTimedRoute() throws Exception {
        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "AAA", RouteType.FASTEST, "2026-10-20T08:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJourneyMinutes").value(0.0))
                .andExpect(jsonPath("$.journeyStartUtc").value("2026-10-20T08:00:00Z"))
                .andExpect(jsonPath("$.journeyArrivalUtc").value("2026-10-20T08:00:00Z"))
                .andExpect(jsonPath("$.legs.length()").value(0));
    }

    @Test
    void fastestUpstreamFailurePreservesBadGatewaySemantics() throws Exception {
        upstreamData.fail = true;

        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "BBB", RouteType.FASTEST, "2026-10-20T08:00:00")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Route graph unavailable"));
    }

    @Test
    void upstreamFailurePreservesBadGatewaySemantics() throws Exception {
        upstreamData.fail = true;

        mockMvc.perform(post("/api/routes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("AAA", "BBB", RouteType.SHORTEST)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Route graph unavailable"))
                .andExpect(jsonPath("$.detail").value("test upstream failure"));
    }

    private String request(String origin, String destination, RouteType routeType) {
        return """
                {
                  "originAirportCode": "%s",
                  "destinationAirportCode": "%s",
                  "routeType": "%s"
                }
                """.formatted(origin, destination, routeType);
    }

    private String request(
            String origin,
            String destination,
            RouteType routeType,
            String departureLocalDateTime) {
        return """
                {
                  "originAirportCode": "%s",
                  "destinationAirportCode": "%s",
                  "routeType": "%s",
                  "departureLocalDateTime": "%s"
                }
                """.formatted(origin, destination, routeType, departureLocalDateTime);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class UpstreamTestConfiguration {

        @Bean
        @Primary
        TestUpstreamData routeCalculationTestUpstreamData() {
            return new TestUpstreamData();
        }
    }

    static class TestUpstreamData implements
            AirportDataProvider,
            AircraftTypeDataProvider,
            FlightCompanyDataProvider,
            AdjustedFlightLegDataProvider {

        private boolean fail;
        private List<AirportClientResponse> airports;
        private List<AircraftTypeClientResponse> aircraftTypes;
        private List<FlightCompanyClientResponse> companies;
        private List<AdjustedFlightLegClientResponse> flightLegs;

        void reset() {
            fail = false;
            airports = List.of(
                    new AirportClientResponse("AAA", 0, 0, 0),
                    new AirportClientResponse("BBB", 0, 1, 0),
                    new AirportClientResponse("CCC", 1, 0, 0));
            aircraftTypes = List.of(
                    new AircraftTypeClientResponse(1L, "Maker", "Model", 800, 1000, 10000, 2, 100, 3));
            companies = List.of(new FlightCompanyClientResponse("CO"));
            flightLegs = List.of(new AdjustedFlightLegClientResponse(
                    1L,
                    1,
                    FlightDirection.OUTBOUND,
                    "CO",
                    1L,
                    "AAA",
                    "BBB",
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 5),
                    0,
                    5));
        }

        @Override
        public List<AirportClientResponse> getAirports() {
            if (fail) {
                throw new UpstreamServiceException("test upstream failure");
            }
            return airports;
        }

        @Override
        public List<AircraftTypeClientResponse> getAircraftTypes() {
            return aircraftTypes;
        }

        @Override
        public List<FlightCompanyClientResponse> getFlightCompanies() {
            return companies;
        }

        @Override
        public List<AdjustedFlightLegClientResponse> getAdjustedFlightLegs() {
            return flightLegs;
        }
    }
}
