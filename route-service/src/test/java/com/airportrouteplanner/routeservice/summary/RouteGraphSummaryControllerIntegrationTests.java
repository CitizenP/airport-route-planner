package com.airportrouteplanner.routeservice.summary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(RouteGraphSummaryControllerIntegrationTests.UpstreamTestConfiguration.class)
class RouteGraphSummaryControllerIntegrationTests {

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

    @Test
    void summaryEndpointUsesStubbedUpstreamProviders() throws Exception {
        mockMvc.perform(get("/api/route-graph/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.airportCount").value(2))
                .andExpect(jsonPath("$.aircraftTypeCount").value(1))
                .andExpect(jsonPath("$.companyCount").value(1))
                .andExpect(jsonPath("$.edgeCount").value(2))
                .andExpect(jsonPath("$.delayedEdgeCount").value(1))
                .andExpect(jsonPath("$.maximumDelayMinutes").value(5));
    }

    @Test
    void unusableUpstreamDataReturnsBadGateway() throws Exception {
        upstreamData.flightLegs = List.of(upstreamData.leg(
                1, FlightDirection.OUTBOUND, "AAA", "ZZZ", 0));

        mockMvc.perform(get("/api/route-graph/summary"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Route graph unavailable"))
                .andExpect(jsonPath("$.detail").value(
                        "upstream data failed route-graph validation: missing destination airport ZZZ"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class UpstreamTestConfiguration {

        @Bean
        @Primary
        TestUpstreamData testUpstreamData() {
            return new TestUpstreamData();
        }
    }

    static class TestUpstreamData implements
            AirportDataProvider,
            AircraftTypeDataProvider,
            FlightCompanyDataProvider,
            AdjustedFlightLegDataProvider {

        private List<AirportClientResponse> airports;
        private List<AircraftTypeClientResponse> aircraftTypes;
        private List<FlightCompanyClientResponse> companies;
        private List<AdjustedFlightLegClientResponse> flightLegs;

        void reset() {
            airports = List.of(
                    new AirportClientResponse("AAA", 0, 0, 0),
                    new AirportClientResponse("BBB", 0, 1, 60));
            aircraftTypes = List.of(
                    new AircraftTypeClientResponse(1L, "Maker", "Model", 800, 1000, 10000, 3, 100, 3));
            companies = List.of(new FlightCompanyClientResponse("CO"));
            flightLegs = List.of(
                    leg(1, FlightDirection.OUTBOUND, "AAA", "BBB", 0),
                    leg(1, FlightDirection.RETURN, "BBB", "AAA", 5));
        }

        AdjustedFlightLegClientResponse leg(
                long routeId,
                FlightDirection direction,
                String origin,
                String destination,
                long delayMinutes) {
            return new AdjustedFlightLegClientResponse(
                    routeId,
                    1,
                    direction,
                    "CO",
                    1L,
                    origin,
                    destination,
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 0).plusMinutes(delayMinutes),
                    0,
                    delayMinutes);
        }

        @Override
        public List<AirportClientResponse> getAirports() {
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
