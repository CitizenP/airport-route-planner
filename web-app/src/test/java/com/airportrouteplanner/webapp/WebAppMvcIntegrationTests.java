package com.airportrouteplanner.webapp;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.airportrouteplanner.webapp.airport.AirportClientResponse;
import com.airportrouteplanner.webapp.airport.AirportDataProvider;
import com.airportrouteplanner.webapp.airport.AirportServiceUnavailableException;
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
@Import(WebAppMvcIntegrationTests.AirportTestConfiguration.class)
class WebAppMvcIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StubAirportDataProvider airportDataProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        airportDataProvider.available = true;
    }

    @Test
    void homePageRendersApplicationShellMapAndPlannerControls() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Airport Route Planner")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"airport-map\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"origin-airport\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"destination-airport\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"SHORTEST\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"FASTEST\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"CHEAPEST\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"ECOLOGICAL\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Calculate route — coming soon")));
    }

    @Test
    void airportBffReturnsSortedStubbedBrowserFacingData() throws Exception {
        mockMvc.perform(get("/api/ui/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].iataCode").value("DEL"))
                .andExpect(jsonPath("$[0].name").value("Indira Gandhi International Airport"))
                .andExpect(jsonPath("$[0].formattedUtcOffset").value("UTC+5:30"))
                .andExpect(jsonPath("$[1].iataCode").value("LIS"))
                .andExpect(jsonPath("$[1].numberOfRunways").value(2))
                .andExpect(jsonPath("$[1].formattedUtcOffset").value("UTC+0"));
    }

    @Test
    void airportTransportFailureBecomesBadGatewayWhileHomeRemainsAvailable() throws Exception {
        airportDataProvider.available = false;

        mockMvc.perform(get("/api/ui/airports"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Airport data unavailable"))
                .andExpect(jsonPath("$.detail").value("test airport-service failure"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AirportTestConfiguration {

        @Bean
        @Primary
        StubAirportDataProvider stubAirportDataProvider() {
            return new StubAirportDataProvider();
        }
    }

    static class StubAirportDataProvider implements AirportDataProvider {

        private boolean available = true;

        @Override
        public List<AirportClientResponse> getAirports() {
            if (!available) {
                throw new AirportServiceUnavailableException("test airport-service failure");
            }
            return List.of(
                    new AirportClientResponse(
                            "LIS", "Humberto Delgado Airport", "Lisbon", "Portugal",
                            38.7742, -9.1342, 2, 0),
                    new AirportClientResponse(
                            "DEL", "Indira Gandhi International Airport", "Delhi", "India",
                            28.5562, 77.1, 4, 330));
        }
    }
}
